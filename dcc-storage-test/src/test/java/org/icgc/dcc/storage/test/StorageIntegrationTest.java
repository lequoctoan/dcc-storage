package org.icgc.dcc.storage.test;

import static com.google.common.base.Strings.repeat;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.icgc.dcc.storage.test.util.Assertions.assertDirectories;
import static org.icgc.dcc.storage.test.util.SpringBootProcess.bootRun;

import java.io.File;
import java.util.List;

import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.MetadataClient;
import org.icgc.dcc.storage.test.auth.AuthClient;
import org.icgc.dcc.storage.test.fs.FileSystem;
import org.icgc.dcc.storage.test.mongo.Mongo;
import org.icgc.dcc.storage.test.s3.S3;
import org.icgc.dcc.storage.test.util.Port;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.val;

public class StorageIntegrationTest {

  /**
   * Configuration.
   */
  final int authPort = 8443;
  final int metadataPort = 8444;
  final int storagePort = 5431;
  final String gnosId = "70b07570-0571-11e5-a6c0-1697f925ec7b";

  /**
   * State.
   */
  final Mongo mongo = new Mongo();
  final S3 s3 = new S3();
  final FileSystem fs = new FileSystem(new File("target/test"), gnosId);

  @Before
  public void setUp() throws Exception {
    banner("Starting file system...");
    fs.start();

    banner("Starting Mongo...");
    mongo.start();

    banner("Starting S3...");
    s3.start();

    banner("Starting dcc-auth-server...");
    authServer();

    banner("Starting dcc-metadata-server...");
    metadataServer();

    banner("Starting dcc-storage-server...");
    storageServer();

    banner("Waiting for service ports...");
    waitForPort(authPort);
    waitForPort(metadataPort);
    waitForPort(storagePort);
  }

  @After
  public void tearDown() {
    s3.stop();
    mongo.stop();
  }

  @Test
  public void test() throws InterruptedException {

    //
    // Authorize
    //

    banner("Authorizing...");
    val accessToken = new AuthClient("https://localhost:" + authPort).createAccessToken();

    //
    // Register
    //

    banner("Registering...");
    val register = metadataClient(accessToken,
        "-i", fs.getUploadsDir() + "/" + gnosId,
        "-m", "manifest.txt",
        "-o", fs.getRootDir().toString());
    register.waitFor(1, MINUTES);

    //
    // Upload
    //

    banner("Uploading...");
    val upload = storageClient(accessToken,
        "upload",
        "--manifest", fs.getRootDir() + "/manifest.txt");
    upload.waitFor(1, MINUTES);

    //
    // Download
    //

    val entities = findEntities(gnosId);
    for (val entity : entities) {
      if (entity.getFileName().endsWith(".bai")) {
        // Skip BAI files since these will be downloaded when the BAM file is requested
        continue;
      }

      banner("Downloading " + entity);
      val download = storageClient(accessToken,
          "download",
          "--object-id", entity.getId(),
          "--output-layout", "bundle",
          "--output-dir", fs.getDownloadsDir().toString());
      download.waitFor(1, MINUTES);
    }

    //
    // Verify
    //
    banner("Verifying...");
    assertDirectories(fs.getDownloadsDir(), fs.getUploadsDir());
  }

  private void authServer() {
    bootRun(
        org.icgc.dcc.auth.server.ServerMain.class,
        "--spring.profiles.active=dev,no_scope_validation", // Don't validate if user has scopes
        "--logging.file=" + fs.getLogsDir() + "/dcc-auth-server.log",
        "--server.port=" + authPort,
        "--management.port=8543",
        "--endpoints.jmx.domain=auth");
  }

  private void metadataServer() {
    bootRun(
        org.icgc.dcc.metadata.server.ServerMain.class,
        "--spring.profiles.active=development",
        "--logging.file=" + fs.getLogsDir() + "/dcc-metadata-server.log",
        "--server.port=" + metadataPort,
        "--management.port=8544",
        "--endpoints.jmx.domain=metadata",
        "--auth.server.url=https://localhost:" + authPort + "/oauth/check_token",
        "--auth.server.clientId=metadata",
        "--auth.server.clientsecret=pass",
        "--spring.data.mongodb.uri=mongodb://localhost:" + mongo.getPort() + "/dcc-metadata");
  }

  private void storageServer() {
    bootRun(
        resolveJarFile("dcc-storage-server"),
        "--spring.profiles.active=dev,default",
        "--logging.file=" + fs.getLogsDir() + "/dcc-storage-server.log",
        "--server.port=" + storagePort,
        "--auth.server.url=https://localhost:" + authPort + "/oauth/check_token",
        "--auth.server.clientId=storage",
        "--auth.server.clientsecret=pass",
        "--metadata.url=https://localhost:" + metadataPort,
        "--endpoints.jmx.domain=storage");
  }

  private Process metadataClient(String accessToken, String... args) {
    return bootRun(
        org.icgc.dcc.metadata.client.ClientMain.class,
        args,
        "--spring.profiles.active=development",
        "--logging.file=" + fs.getLogsDir() + "/dcc-metadata-client.log",
        "--client.upload.servicePort=" + storagePort,
        "--server.baseUrl=https://localhost:" + metadataPort,
        "--accessToken=" + accessToken);
  }

  private Process storageClient(String accessToken, String... args) {
    return bootRun(
        resolveJarFile("dcc-storage-client"),
        args,
        "--logging.file=" + fs.getLogsDir() + "/dcc-storage-client.log",
        "--metadata.url=https://localhost:" + metadataPort,
        "--metadata.ssl.enabled=false",
        "--accessToken=" + accessToken);
  }

  private List<Entity> findEntities(String gnosId) {
    val metadataClient = new MetadataClient("https://localhost:" + metadataPort, false);
    return metadataClient.findEntitiesByGnosId(gnosId);
  }

  private static File resolveJarFile(String artifactId) {
    val targetDir = new File("../" + artifactId + "/target");
    return targetDir.listFiles((File file, String name) -> name.startsWith(artifactId) && name.endsWith(".jar"))[0];
  }

  private static void waitForPort(int port) {
    new Port("localhost", port).waitFor(1, MINUTES);
  }

  private static void banner(String text) {
    System.out.println("");
    System.out.println(repeat("#", 100));
    System.out.println(text);
    System.out.println(repeat("#", 100));
    System.out.println("");
  }

}
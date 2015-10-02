/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.storage.client.fs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Iterator;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * See http://stackoverflow.com/questions/22966176/creating-a-custom-filesystem-implementation-in-java/32887126#32887126
 */
@ToString
@RequiredArgsConstructor
public class StoragePath implements Path {

  private final StorageFileSystem fileSystem;
  private final String path;

  @Override
  public FileSystem getFileSystem() {
    return fileSystem;
  }

  @Override
  public boolean isAbsolute() {
    return true;
  }

  @Override
  public Path getRoot() {
    return root();
  }

  private StoragePath root() {
    return new StoragePath(fileSystem, "/");
  }

  @Override
  public Path getFileName() {
    return root();
  }

  @Override
  public Path getParent() {
    return root();
  }

  @Override
  public int getNameCount() {
    return 1;
  }

  @Override
  public Path getName(int index) {
    return root();
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    return root();
  }

  @Override
  public boolean startsWith(Path other) {
    return true;
  }

  @Override
  public boolean startsWith(String other) {
    return true;
  }

  @Override
  public boolean endsWith(Path other) {
    return true;
  }

  @Override
  public boolean endsWith(String other) {
    return true;
  }

  @Override
  public Path normalize() {
    return root();
  }

  @Override
  public Path resolve(Path other) {
    return root();
  }

  @Override
  public Path resolve(String other) {
    return root();
  }

  @Override
  public Path resolveSibling(Path other) {
    return root();
  }

  @Override
  public Path resolveSibling(String other) {
    return root();
  }

  @Override
  public Path relativize(Path other) {
    return root();
  }

  @Override
  @SneakyThrows
  public URI toUri() {
    return new URI(fileSystem.provider().getScheme() + "://" + path);
  }

  @Override
  public Path toAbsolutePath() {
    return root();
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    return root();
  }

  @Override
  public File toFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
    return null;
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
    return null;
  }

  @Override
  public Iterator<Path> iterator() {
    return Collections.singleton((Path) root()).iterator();
  }

  @Override
  public int compareTo(Path other) {
    return 0;
  }

}
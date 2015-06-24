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
package collaboratory.storage.object.store.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resource service configuration file.<br>
 * Protects resources with access token obtained at the authorization server.
 */
@Configuration
public class SecurityConfig {

  @Configuration
  @EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class, ManagementSecurityAutoConfiguration.class })
  protected static class DefaultSecurityConfig {}

  @Profile("secure")
  @Configuration
  @EnableWebSecurity
  @EnableResourceServer
  @EnableAutoConfiguration
  protected static class EnabledSecurityConfig extends ResourceServerConfigurerAdapter {

    private TokenExtractor tokenExtractor = new BearerTokenExtractor();

    @Override
    public void configure(HttpSecurity http) throws Exception {
      http.addFilterAfter(new OncePerRequestFilter() {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
          // We don't want to allow access to a resource with no token so clear
          // the security context in case it is actually an OAuth2Authentication
          if (tokenExtractor.extract(request) == null) {
            SecurityContextHolder.clearContext();
          }
          filterChain.doFilter(request, response);
        }

      }, AbstractPreAuthenticatedProcessingFilter.class);

      http.csrf().disable();
      configureAuthorization(http);
    }

    @Bean
    public AccessTokenConverter accessTokenConverter() {
      return new DefaultAccessTokenConverter();
    }

    @Bean
    public RemoteTokenServices remoteTokenServices(final @Value("${auth.server.url}") String checkTokenUrl,
        final @Value("${auth.server.clientId}") String clientId,
        final @Value("${auth.server.clientSecret}") String clientSecret) {
      final RemoteTokenServices remoteTokenServices = new RemoteTokenServices();
      remoteTokenServices.setCheckTokenEndpointUrl(checkTokenUrl);
      remoteTokenServices.setClientId(clientId);
      remoteTokenServices.setClientSecret(clientSecret);
      remoteTokenServices.setAccessTokenConverter(accessTokenConverter());

      return remoteTokenServices;
    }

    private static void configureAuthorization(HttpSecurity http) throws Exception {
      // FIXME: Configure access to resources by token scope

      // @formatter:off
      http
        .authorizeRequests()
        .antMatchers("/upload/**")
        .access("#oauth2.hasScope('${auth.server.uploadScope}')")
//        .access("#oauth2.hasScope('s3.upload')")
        .and()
        
        .authorizeRequests()
        .antMatchers("/download/**")
        .access("#oauth2.hasScope('${auth.server.downloadScope}')")
//        .access("#oauth2.hasScope('s3.download')")
        .and()
        
        .authorizeRequests()
        .anyRequest()
        .authenticated();
      // @formatter:on
      System.out.println("Here");
    }
  }

}
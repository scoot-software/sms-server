/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.server.config;

import com.scooter1556.sms.server.database.UserDatabase;
import com.scooter1556.sms.server.utilities.DatabaseUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    private static final String CLASS_NAME = "SecurityConfig";
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
            http
                    .csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/admin/**").hasRole("ADMIN")
                    .antMatchers("/user", "/user/**").hasRole("USER")
                    .antMatchers("/media/**").hasRole("USER")
                    .antMatchers("/media/files").hasRole("ADMIN")
                    .antMatchers("/settings/**").hasRole("USER")
                    .antMatchers("/job/end/**").permitAll()
                    .antMatchers("/job/**").hasRole("USER")
                    .antMatchers("/stream/**").permitAll()
                    .antMatchers("/image/**").permitAll()
                    .antMatchers("/session/active").hasRole("ADMIN")
                    .antMatchers("/session/initialise").hasRole("USER")
                    .antMatchers("/session/end/**").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic()
                    .and()
                    .sessionManagement().sessionCreationPolicy(STATELESS);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {}
    
    @Configuration
    protected static class AuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {
        @Override
        public void init(AuthenticationManagerBuilder auth) throws Exception {
            auth
                .jdbcAuthentication().dataSource(DatabaseUtils.getDataSource(UserDatabase.DB_NAME, UserDatabase.DB_VERSION))
		.usersByUsernameQuery(
			"select Username,Password,Enabled from User where Username=?")
		.authoritiesByUsernameQuery(
			"select Username,Role from UserRole where Username=?");
        }
    }
}
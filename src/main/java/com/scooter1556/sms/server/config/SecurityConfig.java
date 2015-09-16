/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.config;

import com.scooter1556.sms.server.database.UserDatabase;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 *
 * @author scott2ware
 */

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
                    .antMatchers("/settings/**").hasRole("USER")
                    .antMatchers("/job/**").hasRole("USER")
                    .antMatchers("/hls").hasRole("USER")
                    .antMatchers("/hls/**").permitAll()
                    .antMatchers("/dash").hasRole("USER")
                    .antMatchers("/dash/**").permitAll()
                    .antMatchers("/stream/initialise/**").hasRole("USER")
                    .antMatchers("/stream/**").permitAll()
                    .antMatchers("/image/**").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic()
                    .and()
                    .sessionManagement().sessionCreationPolicy(STATELESS);
    }
    
    @Override
    public void configure(WebSecurity web) throws Exception {

    }
 
    @Configuration
    protected static class AuthenticationConfiguration extends
            GlobalAuthenticationConfigurerAdapter {

        @Override
        public void init(AuthenticationManagerBuilder auth) throws Exception {
            auth
                .jdbcAuthentication().dataSource(UserDatabase.getDataSource())
		.usersByUsernameQuery(
			"select Username,Password,Enabled from User where Username=?")
		.authoritiesByUsernameQuery(
			"select Username,Role from UserRole where Username=?");
        }

    }
}
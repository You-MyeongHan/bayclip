package com.bayclip.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final JwtAuthenticationFilter jwtAuthFilter;
	private final AuthenticationProvider authenticationProvider;
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
		http
			// CSRF 설정
			.csrf(AbstractHttpConfigurer::disable)
			
			.authorizeHttpRequests((authorizeRequests) ->
            	authorizeRequests
            	.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
            	
            	//auth 요청
            	.requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
            	.requestMatchers(HttpMethod.POST, "/api/auth/authecticate").permitAll()
            	.requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
            	.requestMatchers(HttpMethod.GET, "/api/auth/checkUid").permitAll()
            	.requestMatchers(HttpMethod.GET, "/api/auth/checkEmail").permitAll()
            	.requestMatchers(HttpMethod.GET, "/api/auth/refresh-token").authenticated()
            	
            	//board 요청
            	.requestMatchers(HttpMethod.GET, "/api/board/{boardId}").permitAll()
            	.requestMatchers(HttpMethod.POST, "/api/board/register").authenticated()
            	.requestMatchers(HttpMethod.GET, "/api/board/post").permitAll()
            	.requestMatchers(HttpMethod.GET, "/api/board/best-post").permitAll()
            	.requestMatchers(HttpMethod.GET, "/api/board/recommendBoard").authenticated()
            	.requestMatchers(HttpMethod.POST, "/api/board/{boardId}/comment").authenticated()
            	.requestMatchers(HttpMethod.DELETE, "/api/board/{boardId}/comment/delete/{commentId}").authenticated()
            	.anyRequest().hasAnyRole("ADMIN"))		
			
			.sessionManagement((sessionManagement) ->
            	sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			
			.authenticationProvider(authenticationProvider)
			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
}
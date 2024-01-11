package com.global.config;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.clipmk.user.entity.User;
import com.clipmk.user.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{
	
	private final TokenProvider tokenProvider;
	private final UserService userService;
	
	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain)throws ServletException, IOException{
		
		final String authHeader=request.getHeader("Authorization");
		String jwt="";
		String id=null;
		
		if (authHeader != null && authHeader.startsWith("Bearer ")) { //액세스 토큰이 있을 때
            jwt = authHeader.substring(7);
            try {
                id = tokenProvider.extractId(jwt); // 액세스 토큰이 있을 때만 추출 시도
            } catch (Exception e) {
                // 액세스 토큰이 없는 경우 무시
            } 
        }
		
		//액세스 토큰이 있을 때
		if(id!=null && SecurityContextHolder.getContext().getAuthentication()==null) {
			User user=userService.loadUserByUsername(id);
			if(tokenProvider.isTokenValid(jwt,user)) {
				UsernamePasswordAuthenticationToken authToken =new UsernamePasswordAuthenticationToken (
					user,
					null,
					user.getAuthorities()
				);
				authToken.setDetails(
					new WebAuthenticationDetailsSource().buildDetails(request)
				);
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}

		}else{ //액세스 토큰이 없을 때
			String refreshToken = getRefreshTokenFromCookie(request);
			
			if(refreshToken==null) {
				filterChain.doFilter(request,response);
				return;
			}
			
			id = tokenProvider.extractId(refreshToken);
			User user=userService.loadUserByUsername(id);
			String accessToken=tokenProvider.generateAccessToken(id,user.getNick(), user.getPoint());
			response.setHeader("Authorization", "Bearer " + accessToken);			
			
			if(tokenProvider.isTokenValid(accessToken,user)) {
				UsernamePasswordAuthenticationToken authToken =new UsernamePasswordAuthenticationToken (
					user,
					null,
					user.getAuthorities()
				);
				authToken.setDetails(
					new WebAuthenticationDetailsSource().buildDetails(request)
				);
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}

		}
		
		filterChain.doFilter(request,response);
	}
	
	private String getRefreshTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
	    if (cookies != null) {
	        for (Cookie cookie : cookies) {
	            if ("refreshToken".equals(cookie.getName())) {
	                return cookie.getValue();
	            }
	        }
	    }
	    return null;
    }
}

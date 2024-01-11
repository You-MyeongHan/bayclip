package com.clipmk.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import com.clipmk.auth.dto.AuthResDto;
import com.clipmk.auth.dto.LoginReqDto;
import com.clipmk.user.repository.UserRepository;
import com.global.config.TokenProvider;
import com.global.error.errorCode.AuthErrorCode;
import com.global.error.exception.RestApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements LogoutHandler{
	
	private final UserRepository userRepository;
	private final TokenProvider tokenProvider;
	private final AuthenticationManager authenticationManager;

	public AuthResDto authenticate(LoginReqDto request) {
		var user=userRepository.findByUid(request.getUid())
				.orElseThrow(()-> new RestApiException(AuthErrorCode.INVALID_CREDENTIALS));
		
		try {
		    authenticationManager.authenticate(
		        new UsernamePasswordAuthenticationToken(
		            request.getUid(),
		            request.getPwd()
		        )
		    );
		} catch (BadCredentialsException e) {
		    throw new RestApiException(AuthErrorCode.INVALID_CREDENTIALS);
		}
		
		var accessToken=tokenProvider.generateAccessToken(user.getId().toString(), user.getNick(), user.getPoint());
		var refreshToken = tokenProvider.generateRefreshToken(user.getId().toString());
		
		return AuthResDto.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.id(user.getId())
				.nick(user.getNick())
				.email(user.getEmail())
				.build();
	}
	
	@Override
	public void logout(
			HttpServletRequest request,
		    HttpServletResponse response,
		    Authentication authentication
	){
		
		final String authHeader = request.getHeader("Authorization");
	    
	    if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
	      return;
	    }
	    SecurityContextHolder.clearContext();
	}
	
}

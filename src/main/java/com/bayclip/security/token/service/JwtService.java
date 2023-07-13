package com.bayclip.security.token.service;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.bayclip.user.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
	private static final String REDIS_KEY_PREFIX = "";
	
	@Value("${application.security.jwt.secret-key}")
	private String secretKey;
	@Value("${application.security.jwt.access-token.expiration}")
	private long accessExpiration;
	@Value("${application.security.jwt.refresh-token.expiration}")
	private long refreshExpiration;
	
	private final RedisTemplate<String, String> redisTemplate;
	
	public String extractId(String token) {
		return extractClaim(token,Claims::getSubject);
	}
	
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims= extractAllClaims(token);
		return claimsResolver.apply(claims);
	}
	
	public String generateToken(
			Map<String, Object> extraClaims,
			String user_id,
			long expiration
	) {
		return  Jwts
				.builder()
				.setClaims(extraClaims)
				.setSubject(user_id)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis()+expiration))
				.signWith(getSignInKey(), SignatureAlgorithm.HS256)
				.compact();
	}
	
	public String generateRefreshToken(String user_id) {
		HashMap<String, Object> map = new HashMap<>();
		String refreshToken=generateToken(map, user_id, refreshExpiration);
		
		saveToken(user_id, refreshToken);
		
		return refreshToken;
	}
	
	public String generateAccessToken(String user_id) {
		HashMap<String, Object> map = new HashMap<>();
		return generateToken(map, user_id, accessExpiration); 
	}
	
	public boolean isTokenValid(String token, User user) {
		final Integer id=Integer.parseInt(extractId(token));
		return (id.equals(user.getId())) && !isTokenExpired(token);
	}
	
	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		return Jwts
				.parserBuilder()
				.setSigningKey(getSignInKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
	
	private Key getSignInKey() {
  		byte[] keyBytes=Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
	
	public void saveToken(String userId, String token) {
        String key = REDIS_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, refreshExpiration, TimeUnit.MILLISECONDS);
    }

    public void setValues(String key, String data) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(key, data);
    }

    public void setValues(String key, String data, Duration duration) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(key, data, duration);
    }

    public String getValues(String key) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        return values.get(key);
    }

    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }
    
    public boolean isTokenValid(String token) {
        String key = REDIS_KEY_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
    public void invalidateToken(String userId) {
        String key = REDIS_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
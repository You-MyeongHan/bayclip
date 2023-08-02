package com.bayclip.mail.service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Service
@RequiredArgsConstructor
public class MailService {
	
    private final StringRedisTemplate stringRedisTemplate;
    
    @Value("${aws.ses.sender}")
	private String senderEmail;
    
    @Value("${aws.ses.access-key}")
	private String accessKey;
    
    @Value("${aws.ses.secret-key}")
	private String secretKey;
    
    private static final int EXPIRATION_MINUTES = 5;
    
    public void sendVerificationEmail(String to){
    	
    	//인증번호 생성
    	String verificationCode = generateVerificationCode();
    	//
    	stringRedisTemplate.opsForValue().set(to, verificationCode, EXPIRATION_MINUTES, TimeUnit.MINUTES);
    	
    	// 이메일 발송
        sendEmailWithSES(to, verificationCode);
    }
	
	private String generateVerificationCode() {
		
        int codeLength = 6;
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < codeLength; i++) {
            sb.append(random.nextInt(10));
        }
        
        return sb.toString();
    }
	
	private void sendEmailWithSES(String to, String verificationCode) {
		
		AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
		SesClient sesClient = SesClient.builder()
                .region(Region.AP_NORTHEAST_2) // AWS SES 리전 설정
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
		
		// 이메일 발송 요청 생성
        Destination destination = Destination.builder().toAddresses(to).build();
        Content subject = Content.builder().data("이메일 인증 코드").build();
        Content body = Content.builder().data("인증 코드: " + verificationCode).build();
        Body emailBody = Body.builder().text(body).build();
        Message message = Message.builder().subject(subject).body(emailBody).build();
        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(destination)
                .message(message)
                .build();

        // 이메일 발송
        sesClient.sendEmail(emailRequest);

        // 클라이언트 종료
        sesClient.close();
    }
}

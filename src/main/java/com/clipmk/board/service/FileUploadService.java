package com.clipmk.board.service;

import java.io.File;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.clipmk.user.entity.User;

import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkClientException;

@Service
@RequiredArgsConstructor
public class FileUploadService {
	@Value("${cloud.aws.s3.bucket.url}")
    private String defaultUrl;

    @Value("${cloud.aws.s3.bucket.name}")
    private String bucket;
    
    @Value("${cloud.aws.s3.bucket.image.postTemp}")
    private String postTemp;
    
    @Value("${cloud.aws.s3.bucket.image.post}")
    private String post;
    
    private final AmazonS3Client amazonS3Client;

    public String upload(MultipartFile uploadFile, User user) throws IOException {

        String origName = uploadFile.getOriginalFilename();
        String url;
        try {
            String tempFolder = postTemp + user.getId() + "/";
            
            // 파일이름 암호화, 확장자 찾기
            final String saveFileName = getUuid() + origName.substring(origName.lastIndexOf('.'));
            // 파일 객체 생성
            // System.getProperty => 시스템 환경에 관한 정보를 얻을 수 있다. (user.dir = 현재 작업 디렉토리를 의미함)
            File file = new File(System.getProperty("user.dir") + saveFileName);
            // 파일 변환
            try {
				uploadFile.transferTo(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
            // S3 파일 업로드
            uploadOnS3(saveFileName, file, tempFolder);
            // 주소 할당
            url = defaultUrl + tempFolder+saveFileName;
            // 파일 삭제
            file.delete();
        } catch (StringIndexOutOfBoundsException e) {
            url = null;
        }
        return url;
    }

    private static String getUuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private void uploadOnS3(final String fileName, final File file, String dirName) {
        // AWS S3 전송 객체 생성
        final TransferManager transferManager = TransferManagerBuilder.standard()
                                                .withS3Client(this.amazonS3Client).build();
        // 요청 객체 생성
        final PutObjectRequest request = new PutObjectRequest(bucket, dirName + fileName, file);
        // 업로드 시도
        final Upload upload =  transferManager.upload(request);

        try {
            upload.waitForCompletion();
        } catch (AmazonClientException | InterruptedException amazonClientException) {
            amazonClientException.printStackTrace();
        }
    }
    
    public void delete(String imageUrl) {
        try {
            final String deleteFileName = imageUrl.substring(imageUrl.lastIndexOf('/')+1);
            amazonS3Client.deleteObject(this.bucket, deleteFileName);
        } catch (SdkClientException e) {
            e.printStackTrace();
        }
    }
}

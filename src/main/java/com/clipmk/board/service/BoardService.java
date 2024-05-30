package com.clipmk.board.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3Client;
import com.clipmk.board.dto.CommentDto;
import com.clipmk.board.dto.EditPostReqDto;
import com.clipmk.board.dto.PostDto;
import com.clipmk.board.dto.PostReqDto;
import com.clipmk.board.entity.Comment;
import com.clipmk.board.entity.Post;
import com.clipmk.board.repository.BoardRepository;
import com.clipmk.board.repository.CommentRepository;
import com.clipmk.user.entity.User;
import com.clipmk.user.repository.UserRepository;
import com.global.error.errorCode.BoardErrorCode;
import com.global.error.errorCode.UserErrorCode;
import com.global.error.exception.RestApiException;
import com.infra.meta.dto.PostIdDto;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {
	
	private final BoardRepository boardRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final AmazonS3Client amazonS3Client;
	@Value("${point.recommend-post}")
	private int RPP;	//Recommend Post Point
	@Value("${point.create-post}")
	private int CPP;	//Recommend Post Point 
	@Value("${cloud.aws.s3.bucket.url}")
    private String defaultUrl;
	@Value("${cloud.aws.s3.bucket.name}")
    public String bucket;
	
	@Transactional
	public PostDto getPostById(Long postId, User user, HttpSession session) {
		
		Post post=boardRepository.findById(postId).orElseThrow(
				()-> new RestApiException(BoardErrorCode.POST_NOT_FOUND));
		Set<Long> viewedPostIds = (Set<Long>) session.getAttribute("viewedPostIds");
		
		if (viewedPostIds == null) {
            viewedPostIds = new HashSet<>();
        }
		
		if (!viewedPostIds.contains(postId)) {
        	post.setViewCnt(post.getViewCnt()+1);
            boardRepository.save(post);
            viewedPostIds.add(postId);
        }
		session.setAttribute("viewedPostIds", viewedPostIds);
		

		PostDto postDto = post.toDto();
		
		List<Comment> comments = commentRepository.findByPostIdAndParentIsNull(postId);
		List<CommentDto> commentDtos = new ArrayList<>();
		
		for (Comment comment : comments) {
            CommentDto commentDto = CommentDto.from(comment);
            List<CommentDto> replyDtos = comment.getReplies();
            commentDto.setReplies(replyDtos);
            commentDtos.add(commentDto);
        }

		postDto.setComments(commentDtos);
		if(user!=null) {
			if(post.getRecommendations().contains(user.getId()))
				postDto.setRecommend_state(1);
			else if(post.getDecommendations().contains(user.getId()))
				postDto.setRecommend_state(-1);
			else
				postDto.setRecommend_state(0);
		}
		
		return postDto;
        
	}
	
//	public Long getBoardCnt(Integer userId) {
//		
//		return boardRepository.countByUser_Id(userId);
//	}
	
	@Transactional
	public PostDto edit(Long postId, EditPostReqDto request, User user) {
		
		Post post= boardRepository.findById(postId).orElseThrow(
				()-> new RestApiException(BoardErrorCode.POST_NOT_FOUND));
		
		if (!post.getUser().getId().equals(user.getId())) {
	        throw new RestApiException(BoardErrorCode.POST_ACCESS_DENIED);
	    }
		
		if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getTable() != null) {
            post.setTbl(request.getTable());
        }
        if (request.getGroup() != null) {
            post.setGrp(request.getGroup());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getThumbnail() != null) {
            post.setThumbnail(request.getThumbnail());
        }
        
		Post newPost = boardRepository.save(post);
		
		return newPost.toDto();
	}
	
	@Transactional
	public void delete(Long postId, User user) {
		
		Post post= boardRepository.findById(postId).orElseThrow(
				()-> new RestApiException(BoardErrorCode.POST_NOT_FOUND));
		
		if (!post.getUser().getId().equals(user.getId())) {
	        throw new RestApiException(BoardErrorCode.POST_ACCESS_DENIED);
	    }
		
		boardRepository.delete(post);
	}
	
	public Page<Post> findAll(Specification<Post> spec, Pageable pageable) {
		return boardRepository.findAll(spec, pageable);
	}
	
	public Page<Post> findByViewCntGreaterThan(Integer viewCount, Pageable pageable){
		return boardRepository.findByViewCntGreaterThan(viewCount, pageable);
	}
	
	public Page<Post> findTop10ByUser_IdOrderByIdDesc(Integer userId, Pageable pageable){
		return boardRepository.findTop10ByUser_IdOrderByIdDesc(userId, pageable);
	}
	
	@Transactional
	public boolean register(PostReqDto request, Integer userId) {
		
		User user=userRepository.findById(userId).orElseThrow(
				()-> new RestApiException(UserErrorCode.USER_NOT_FOUND));
		
		String htmlContent=parseContentAndMoveImages(request.getContent());
		Document doc = Jsoup.parse(htmlContent);
		String thumbnail = null;
		Elements imgTags = doc.select("img");
		if (imgTags != null) {
            String srcAttribute = imgTags.attr("src");
            if (srcAttribute.contains("https://clipmarkets3.s3.ap-northeast-2.amazonaws.com/posts/images/main")) {
                thumbnail = srcAttribute;
            }
        }
		
		Integer point = user.getPoint();
		Post post=Post.builder()
				.title(request.getTitle())
				.tbl(request.getTable())
				.grp(request.getGroup())
				.content(htmlContent)
				.user(user)
				.thumbnail(thumbnail)
				.build();
		point+=CPP;
		
		user.setPoint(point);
		userRepository.save(user);
		boardRepository.save(post);
		
		return true;
	}
	
	private String parseContentAndMoveImages(String content) {
		Document doc = Jsoup.parse(content);
		Elements images = doc.getElementsByTag("img");
		
		if (images.size() > 0) {
	        for (Element image : images) {
	            String source = image.attr("src");

	            if (!source.contains("/temp/")) {
	                continue;
	            }

	            source = source.replace(defaultUrl, "");
	            String newSource = "posts/images/main/"+LocalDate.now().toString() + "/" + source.split("/")[4];

	            content = content.replace(source, newSource);

	            update(source, newSource);

	        }
	    }
		
		return content;
	}
	
	public void update(String oldSource, String newSource) {
	    try {
	        oldSource = URLDecoder.decode(oldSource, "UTF-8");
	        newSource = URLDecoder.decode(newSource, "UTF-8");
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    }

	    moveS3(oldSource, newSource);
	    deleteS3(oldSource);
	}
	
	private void moveS3(String oldSource, String newSource) {
		amazonS3Client.copyObject(bucket, oldSource, bucket, newSource);
	}

	private void deleteS3(String source) {
		amazonS3Client.deleteObject(bucket, source);
	}
	
	@Transactional
	public void updateView_cnt(Long postId) {
		Post post=boardRepository.findById(postId).orElseThrow(()->
			new IllegalStateException("게시물이 존재하지 않습니다."));
		
		post.updateViewCnt(post.getViewCnt());
	}
	
	@Transactional
	public void recommend(Long postId, User user, int value) {
		
		Post post= boardRepository.findById(postId).orElseThrow(
				()-> new RestApiException(BoardErrorCode.POST_NOT_FOUND));
		if(user==null) {
			throw new RestApiException(UserErrorCode.USER_NOT_FOUND);
		}
		User postUser=post.getUser();
			
		Set<Integer> recommendations = post.getRecommendations();
        Set<Integer> decommendations = post.getDecommendations();
        Integer point = postUser.getPoint();
        if (value == 1) {
            // 추천 버튼 클릭
            if(decommendations.remove(user.getId())) {
            	point+=RPP;
            }

            if (recommendations.contains(user.getId())) {
                // 이미 추천한 경우, 추천 취소
                recommendations.remove(user.getId());
                point-=RPP;
            } else {
            	recommendations.add(user.getId());
            	point+=RPP;
            }
        } else if (value == -1) {
            // 비추천 버튼 클릭
            if(recommendations.remove(user.getId())) {
            	point-=RPP;
            }

            if (decommendations.contains(user.getId())) {
                // 이미 비추천한 경우, 비추천 취소
                decommendations.remove(user.getId());
                point+=RPP;
            }
            else{
                // 비추천 추가
            	decommendations.add(user.getId());
            	point-=RPP;
            }
        }
        boardRepository.save(post);
        postUser.setPoint(point);
        userRepository.save(postUser);
	}
	
	public Page<Post> findByUserId(int userId, Pageable pageable){
		Page<Post> posts = boardRepository.findByUserId(userId, pageable);
		return posts;
	}
	
	public List<PostIdDto> getPostsIds(){
		List<PostIdDto> postsIds= boardRepository.findAllPostIds();
		return postsIds;
	}
}
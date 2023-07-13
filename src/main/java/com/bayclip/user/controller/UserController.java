package com.bayclip.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bayclip.board.service.BoardService;
import com.bayclip.board.service.CommentService;
import com.bayclip.user.entity.User;
import com.bayclip.user.entity.UserInfo;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
	
	private final BoardService boardService;
	private final CommentService commentService;
	
	@GetMapping("/getInfo")
	public ResponseEntity<UserInfo> getUserInfo(
			@RequestParam(value="act", defaultValue="userInfo") String act,
			@AuthenticationPrincipal User user){
		
		if(act.equals("userInfo")) {
			UserInfo userInfo=UserInfo.builder()
					.nick(user.getNick())
					.uid(user.getUid())
					.email(user.getEmail())
					.emailReceive(user.getEmailReceive())
					.boardCnt(boardService.getBoardCnt(user.getId()))
					.commentCnt(commentService.getCommentCnt(user.getId()))
					.build();
			return ResponseEntity.ok(userInfo);
		}
		else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
}

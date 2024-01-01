package com.clipmk.board.dto;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

import com.clipmk.barter.dto.ItemsResDto;
import com.clipmk.board.entity.Post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PostsResDto {
	private Long id;
	private String title;
	private String tbl;
	private String grp;
	private String nick;
	private LocalDateTime wr_date;
	private Integer view_cnt;
	private Integer recommend_cnt;
	private Integer comment_cnt;
	private String thumbnail;

	public static PostsResDto from(Post post) {
		PostsResDto postsResDto = PostsResDto.builder()
				.title(post.getTitle())
				.nick(post.getUser().getNick())
				.id(post.getId())
				.tbl(post.getTbl())
				.grp(post.getGrp())
				.wr_date(post.getWr_date())
				.view_cnt(post.getViewCnt())
				.recommend_cnt(post.getRecommendations().size())
				.comment_cnt(post.getComments().size())
				.thumbnail(post.getThumbnail())
				.build();
		return postsResDto;
	}
}
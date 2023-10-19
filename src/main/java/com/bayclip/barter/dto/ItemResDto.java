package com.bayclip.barter.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemResDto {
	private Long id;
	private String title;
	private String category;
	private String content;
	private LocalDateTime  wr_date;
	private LocalDateTime  re_date;
	private LocalDateTime  del_date;
	private Integer viewCnt;
	private String user_nick;
	private int user_id;
	
}

package com.clipmk.barter.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import com.clipmk.region.entity.Region;

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
	private List<String> img_src;
	private LocalDateTime  wr_date;
	private LocalDateTime  re_date;
	private LocalDateTime  del_date;
	private int dib_cnt;
	private boolean dib_state;
	private Integer viewCnt;
	private String user_nick;
	private Region region;
	private int user_id;
	private Page<DealsResDto> deals;
	private Integer status;
	
}

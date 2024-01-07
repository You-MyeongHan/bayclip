package com.clipmk.barter.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemReqDto {
	private String title;
	private String content;
	private String category;
	private String regionCode;
	private String thumbnail;
	private List<String> imgSrc;
}

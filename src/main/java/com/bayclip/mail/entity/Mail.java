package com.bayclip.mail.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mail {
	private String address;
    private String title;
    private String message;
}

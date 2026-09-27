package com.cmc.demo.model.entity.request.ebook;

import io.swagger.v3.oas.annotations.media.Schema;

public class EBookContentProfileSearchRequest {
	@Schema(type = "String",description = "書籍deliverId",example = "1235874123",required = true)
	private String deliverId;

	public String getDeliverId() {
		return deliverId;
	}

	public void setDeliverId(String deliverId) {
		this.deliverId = deliverId;
	}
	
}

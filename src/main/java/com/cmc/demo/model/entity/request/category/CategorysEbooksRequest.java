package com.cmc.demo.model.entity.request.category;


import io.swagger.v3.oas.annotations.media.Schema;

public class CategorysEbooksRequest {

	@Schema(type = "String",description = "查詢分類代碼",example = "1235874123",required = true)
	private String categoryCode;

	public String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}
	
	
	
}

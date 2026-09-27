package com.cmc.demo.model.entity.request.ebook;

import io.swagger.v3.oas.annotations.media.Schema;

public class RefRecommendRequest {
	
	@Schema(type = "String",description = "Momo品號",example = "1235874123",required = true)
	private String goodsCode;
	@Schema(type = "String",description = "所屬分類代碼",example = "1235874123",required = true)
	private String categoryCode;
	@Schema(type = "int",description = "一次查多少品",example = "10",required = false,defaultValue = "40")
	private int topk;
	public String getGoodsCode() {
		return goodsCode;
	}
	public void setGoodsCode(String goodsCode) {
		this.goodsCode = goodsCode;
	}

	public int getTopk() {
		return topk;
	}
	public void setTopk(int topk) {
		this.topk = topk;
	}
	public String getCategoryCode() {
		return categoryCode;
	}
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}
	
	
}

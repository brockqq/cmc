package com.cmc.demo.model.entity.ebook;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public class NewBookAndPopularBook {
	@Schema(type = "String", example = "20231128235959", description = "書單最後同步時間")
	private String syncTime;
	
	@Schema(type = "List<EBook>",description = "新品書單")
	private List<EBook> newBooks;
	@Schema(type = "List<EBook>",description = "熱銷書單")
	private List<EBook> popularBooks;
	public String getSyncTime() {
		return syncTime;
	}
	public void setSyncTime(String syncTime) {
		this.syncTime = syncTime;
	}
	public List<EBook> getNewBooks() {
		return newBooks;
	}
	public void setNewBooks(List<EBook> newBooks) {
		this.newBooks = newBooks;
	}
	public List<EBook> getPopularBooks() {
		return popularBooks;
	}
	public void setPopularBooks(List<EBook> popularBooks) {
		this.popularBooks = popularBooks;
	}
	
	
}

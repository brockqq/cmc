package com.cmc.demo.model.entity.request.search;

import io.swagger.v3.oas.annotations.media.Schema;

public class SearchMyBookRequest {
	@Schema(type = "String",description = "搜尋字串",example = "閱讀器使用",required = true)
	private String keyWord;
	@Schema(type = "String",description = "是否查詢成人內容/寫真館內容",example = "false",required = false,defaultValue = "false")
	private boolean showAdult;
	@Schema(type = "int",description = "從第n筆開始(從1開始)",example = "1",required = false,defaultValue = "10")
	private int startIndex;
	@Schema(type = "int",description = "一次搜尋多少筆(1-999)",example = "1",required = false,defaultValue = "50")
	private int length;
	public String getKeyWord() {
		return keyWord;
	}
	public void setKeyWord(String keyWord) {
		this.keyWord = keyWord;
	}
	public boolean isShowAdult() {
		return showAdult;
	}
	public void setShowAdult(boolean showAdult) {
		this.showAdult = showAdult;
	}
	public int getStartIndex() {
		return startIndex;
	}
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}


	
}

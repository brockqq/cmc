package com.cmc.demo.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;

public class AccuntAuthInfo {
	@Schema(type="String",description = "使用者id",example = "MM_202311210801")
	private String userId ="";
	@Schema(type="String",description = "token",example = "xxxxxxxxxx.xxxxxxxxxx.xxxxxxxxx")
	private String authCode="";
	@Schema(type="String",description = "token 到期時間",example = "2023123123595000")
	private String expiresAt="";

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getAuthCode() {
		return authCode;
	}
	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}
	public String getExpiresAt() {
		return expiresAt;
	}
	public void setExpiresAt(String expiresAt) {
		this.expiresAt = expiresAt;
	}
	


}
//checkCode	String	驗證資訊,檢查流程使用此數據檢查是否已被綁定
//expiresAt	String	到期時間,驗證資訊到期時間。

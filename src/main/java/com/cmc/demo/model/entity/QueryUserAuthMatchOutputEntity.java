package com.cmc.demo.model.entity;

public class QueryUserAuthMatchOutputEntity {
	private String userId;
	private boolean match;
	private String authCode ="";
	

	public QueryUserAuthMatchOutputEntity(String userId, boolean match, String authCode) {
		super();
		this.userId = userId;
		this.match = match;
		this.authCode = authCode;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public boolean isMatch() {
		return match;
	}
	public void setMatch(boolean match) {
		this.match = match;
	}
	public String getAuthCode() {
		return authCode;
	}
	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}
	
}
package com.cmc.demo.model.entity;

import java.math.BigDecimal;

public class UserAuthInfo {
	private String userId;
	private String authCode;
	private BigDecimal amount;

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
	
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public UserAuthInfo(String userId, String authCode, BigDecimal amount) {
		super();
		this.userId = userId;
		this.authCode = authCode;
		this.amount = amount;
	}


}

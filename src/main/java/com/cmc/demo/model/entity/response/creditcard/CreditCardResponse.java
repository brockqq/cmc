package com.cmc.demo.model.entity.response.creditcard;

import java.util.List;

import com.cmc.demo.model.entity.CreditCardInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditCardResponse {
	/**
	 * 用戶ID
	 */
	@Schema(description = "用戶ID")
	private String userId;
	
	/**
	 * 信用卡信息列表
	 */
	@Schema(description = "信用卡信息列表")
	private List<CreditCardInfo> creditCards;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<CreditCardInfo> getCreditCards() {
		return creditCards;
	}

	public void setCreditCards(List<CreditCardInfo> creditCards) {
		this.creditCards = creditCards;
	}
}


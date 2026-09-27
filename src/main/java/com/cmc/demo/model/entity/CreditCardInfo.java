package com.cmc.demo.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreditCardInfo {
	@Schema(type="String",description = "信用卡ID",example = "CARD001")
	private String cardId ="";
	
	@Schema(type="String",description = "卡號（加密顯示）",example = "4532xxxxxxxxxxxx")
	private String cardNumber="";
	
	@Schema(type="String",description = "持卡人名字",example = "John Doe")
	private String cardHolder="";
	
	@Schema(type="String",description = "過期日期",example = "12/26")
	private String expiryDate="";
	
	@Schema(type="String",description = "卡類型",example = "VISA")
	private String cardType="";
	
	@Schema(type="String",description = "狀態",example = "ACTIVE")
	private String status="";

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public String getCardHolder() {
		return cardHolder;
	}

	public void setCardHolder(String cardHolder) {
		this.cardHolder = cardHolder;
	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getCardType() {
		return cardType;
	}

	public void setCardType(String cardType) {
		this.cardType = cardType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}


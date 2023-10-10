package com.cmc.demo.model.entity;

import java.math.BigDecimal;

public class PaymentEntity {

	private String monthKey;
	private String date;
	private String description;
	private BigDecimal amount;
	
	
	
	public PaymentEntity(String monthKey, String date, String description, BigDecimal amount) {
		super();
		this.monthKey = monthKey;
		this.date = date;
		this.description = description;
		this.amount = amount;
	}
	public String getMonthKey() {
		return monthKey;
	}
	public void setMonthKey(String monthKey) {
		this.monthKey = monthKey;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	
	
	
	
}

package com.cmc.demo.model.entity.monthly;

import com.cmc.demo.model.entity.AccuntAuthInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;


@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyServiceEbookGroupResponse {
	@Schema(description = "權限資訊,若沒有權限此物會是空白")
	private AccuntAuthInfo accunt;
	@Schema(description = "月租服務清單")
	private MonthlyServiceEbookGroup monthlyService;
	public AccuntAuthInfo getAccunt() {
		return accunt;
	}
	public void setAccunt(AccuntAuthInfo accunt) {
		this.accunt = accunt;
	}
	public MonthlyServiceEbookGroup getMonthlyService() {
		return monthlyService;
	}
	public void setMonthlyService(MonthlyServiceEbookGroup monthlyService) {
		this.monthlyService = monthlyService;
	}



}

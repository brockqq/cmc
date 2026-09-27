package com.cmc.demo.model.entity.response.monthly;

import java.util.List;

import com.cmc.demo.model.entity.AccuntAuthInfo;
import com.cmc.demo.model.entity.monthly.MonthlyServiceDetail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;


@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyServiceResponse {
	/**
	 * 權限資訊,若沒有權限此物會是空白
	 */
	@Schema(description = "權限資訊,若沒有權限此物會是空白")
	private AccuntAuthInfo accunt;
	@Schema(description = "月租服務清單")
	private List<MonthlyServiceDetail> monthlyServiceDetailList;
	public AccuntAuthInfo getAccunt() {
		return accunt;
	}
	public void setAccunt(AccuntAuthInfo accunt) {
		this.accunt = accunt;
	}
	public List<MonthlyServiceDetail> getMonthlyServiceDetailList() {
		return monthlyServiceDetailList;
	}
	public void setMonthlyServiceDetailList(List<MonthlyServiceDetail> monthlyServiceDetailList) {
		this.monthlyServiceDetailList = monthlyServiceDetailList;
	}


}

package com.cmc.demo.model.entity.monthly;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class MonthlyServiceEbookGroup {
	@Schema(description="服務端最後同步時間",example = "20231121085648")	
	private String syncTime ="20231121085648";
	
	@Schema(description="服務名稱",type ="String",example = "雜誌樂讀包")
	private String serviceName ="雜誌樂讀包";
	
	@Schema(description="服務代碼",type ="String",example = "XXXXXX")
	private String serviceCode ="TAI01013";
	@Schema(description="此服務底下書單",type ="List<MonthlyGroup>",example = "")
	private List<MonthlyGroup> groups =new ArrayList<MonthlyGroup>();
	public String getSyncTime() {
		return syncTime;
	}
	public void setSyncTime(String syncTime) {
		this.syncTime = syncTime;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getServiceCode() {
		return serviceCode;
	}
	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}
	public List<MonthlyGroup> getGroups() {
		return groups;
	}
	public void setGroups(List<MonthlyGroup> groups) {
		this.groups = groups;
	}
	
	
}

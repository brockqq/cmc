package com.cmc.demo.model.entity.monthly;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class MonthlyServiceDetail {
	@Schema(type= "String" , example= "雜誌樂讀包",description = "服務名稱")
	private  String serviceName ="";//
	@Schema(type= "String" , example= "TAI01013",description = "服務代碼")
	private  String serviceCode="";
	@Schema(type= "String" , example= "1",description = "訂閱狀態(1=已訂閱,0=未訂閱)")
	private  String subscribed="";
	@Schema(type= "String" , example= "<span> html code </span>",description = "服務描述，是html code")
	private  String desc="";
	
	@Schema(type= "String" , example= "https://img.mybookuat.momoshop.com.tw/xxxx.png",description = "大圖位置")
	private  String picUrlBig="";
	@Schema(type= "String" , example= "https://img.mybookuat.momoshop.com.tw/xxxx.png",description = "小圖位置")
	private  String picUrlSmall="";
	
	@Schema(type= "Boolean" , example= "false",description = "成人內容/女優包")
	private  String isAdult="";
	
	@Schema(type= "BigDecimal" , example= "150",description = "服務價格")
	private  BigDecimal price =BigDecimal.ZERO;

	
	@Schema(type= "String" , example= "最多當期",description = "簡述")
	private  String nickName="";
	
	
	@Schema(type= "String" , example= "1",description = "")
	private  String catDisplay="";	
	
	@Schema(type= "Boolean" , example= "true",description = "")
	private  Boolean isActive= false;
	@Schema(type= "Boolean" , example= "true",description = "")
	private  Boolean videoDisplay =false;	
	
	@Schema(type= "Boolean" , example= "true",description = "")
	private  Boolean isVideoIcon = false;
	@Schema(type= "Boolean" , example= "true",description = "")
	private  Boolean isGetFree = false;
	
	@Schema(type= "Boolean" , example= "true",description = "")
	private  Boolean isProvideText = false;
	
	@Schema(type= "Boolean" , example= "true",description = "")
	private  Boolean isIAPExp = false;
	
	
	
	
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

	public String getSubscribed() {
		return subscribed;
	}

	public void setSubscribed(String subscribed) {
		this.subscribed = subscribed;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getPicUrlBig() {
		return picUrlBig;
	}

	public void setPicUrlBig(String picUrlBig) {
		this.picUrlBig = picUrlBig;
	}

	public String getPicUrlSmall() {
		return picUrlSmall;
	}

	public void setPicUrlSmall(String picUrlSmall) {
		this.picUrlSmall = picUrlSmall;
	}

	public String getIsAdult() {
		return isAdult;
	}

	public void setIsAdult(String isAdult) {
		this.isAdult = isAdult;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getCatDisplay() {
		return catDisplay;
	}

	public void setCatDisplay(String catDisplay) {
		this.catDisplay = catDisplay;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Boolean getVideoDisplay() {
		return videoDisplay;
	}

	public void setVideoDisplay(Boolean videoDisplay) {
		this.videoDisplay = videoDisplay;
	}

	public Boolean getIsVideoIcon() {
		return isVideoIcon;
	}

	public void setIsVideoIcon(Boolean isVideoIcon) {
		this.isVideoIcon = isVideoIcon;
	}

	public Boolean getIsGetFree() {
		return isGetFree;
	}

	public void setIsGetFree(Boolean isGetFree) {
		this.isGetFree = isGetFree;
	}

	public Boolean getIsProvideText() {
		return isProvideText;
	}

	public void setIsProvideText(Boolean isProvideText) {
		this.isProvideText = isProvideText;
	}

	public Boolean getIsIAPExp() {
		return isIAPExp;
	}

	public void setIsIAPExp(Boolean isIAPExp) {
		this.isIAPExp = isIAPExp;
	}

}

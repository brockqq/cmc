package com.cmc.demo.model.entity.responseinfo;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ResponseObj.java Response Object �q�Ϊ�
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Component("ResponseInfoObjP")
@Schema
public class ResponseInfoObjP {

	@Schema(title = "流程是否成功",type = "Boolean",example = "true")
	@JsonProperty("success")
	private boolean bIsSuccess =true;
	
	@Schema(title = "resultCode",type = "Integer",example = "200")
	@JsonProperty("resultCode")
	private Integer sCode =200;

	@Schema(title = "message",type = "String",example = "process done")
	@JsonProperty("resultMessage")
	private String sMessage="";

//  @JsonProperty("resultException")
	private String sException;

	@Schema(title = "實際處理回傳的entity,若success =false則會是空物件",type = "Object",example = "{}")
	@JsonProperty("resultData")
	private Object oResult;

	/**
	 * get() new ResponseObj()
	 * 
	 * @return ResponseObj the object
	 */
	@JsonIgnore
	public static ResponseInfoObjP get() {
		return new ResponseInfoObjP();
	}

	/**
	 * toMap() �N�����ഫ�� Map<String, Object> ����
	 * 
	 * @return Map<String, Object> Map Object of this
	 */
	@JsonIgnore
	public Map<String, Object> toMap() throws IOException {
		return new ObjectMapper().readValue(toJson(), new TypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * toJson() �N�����ഫ�� Json String
	 * 
	 * @return String Json String of this object
	 */
	@JsonIgnore
	public String toJson() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}

	/**
	 * fromJson(json) �q Json String �ഫ�� ResponseObj ����
	 * 
	 * @param String Json String for the object
	 * @return ResponseObj the object
	 * @throws IOException
	 */
	@JsonIgnore
	public static ResponseInfoObjP fromJson(String s) throws IOException {
		return new ObjectMapper().readValue(s, ResponseInfoObjP.class);
	}

	/**
	 * newInstance(bIsSuccess) ResponseObj Constructor
	 * 
	 * @param bIsSuccess the value of IsSuccess
	 * @return ResponseObj ����
	 */
	@JsonIgnore
	public static ResponseInfoObjP newInstance(boolean bIsSuccess) {
		return ResponseInfoObjP.get().setSuccess(bIsSuccess);
	}

	@JsonIgnore
	public static ResponseInfoObjP newInstance(boolean bIsSuccess, String message) {
		return ResponseInfoObjP.get().setSuccess(bIsSuccess).setMessage(message);
	}

	/**
	 * newInstance(bIsSuccess, oResult) ResponseObj Constructor
	 * 
	 * @param bIsSuccess the value of IsSuccess
	 * @param oResult    the value of Result
	 * @return ResponseObj ����
	 */
	@JsonIgnore
	public static ResponseInfoObjP newInstance(boolean bIsSuccess, Object oResult) {
		return newInstance(bIsSuccess).setResult(oResult);
	}

	/**
	 * getter and setter for ResponseObj
	 */

	@JsonIgnore
	public boolean isSuccess() {
		return bIsSuccess;
	}

	@JsonIgnore
	public ResponseInfoObjP setSuccess(boolean bIsSuccess) {
		this.bIsSuccess = bIsSuccess;
		return this;
	}

	@JsonIgnore
	public int getCode() {
		return sCode;
	}

	@JsonIgnore
	public ResponseInfoObjP setCode(int sCode) {
		this.sCode = sCode;
		return this;
	}

	@JsonIgnore
	public String getMessage() {
		return sMessage;
	}

	@JsonIgnore
	public ResponseInfoObjP setMessage(String sMessage) {
		this.sMessage = sMessage;
		return this;
	}

	@JsonIgnore
	public String getException() {
		return sException;
	}

	@JsonIgnore
	public ResponseInfoObjP setException(String sException) {
		this.sException = sException;
		return this;
	}

	@JsonIgnore
	public Object getResult() {
		return oResult;
	}

	@JsonIgnore
	public ResponseInfoObjP setResult(Object oResult) {
		this.oResult = oResult;
		return this;
	}
}

package com.cmc.demo.model.entity.responseinfo.creditcard;

import com.cmc.demo.model.entity.response.creditcard.CreditCardResponse;
import com.cmc.demo.model.entity.responseinfo.ResponseInfoObjP;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 信用卡查詢Response Info Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditCardResponseInfoObj extends ResponseInfoObjP {

	@JsonProperty("resultData")
	private CreditCardResponse oResult;

	@JsonIgnore
	public CreditCardResponseInfoObj setResult(CreditCardResponse oResult) {
		this.oResult = oResult;
		return this;
	}
}


package com.cmc.demo.model.entity.responseinfo.monthly;


import com.cmc.demo.model.entity.response.monthly.MonthlyServiceResponse;
import com.cmc.demo.model.entity.responseinfo.ResponseInfoObjP;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ResponseObj.java Response Object �q�Ϊ�
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyServiceResponseInfoObj extends ResponseInfoObjP{


  @JsonProperty("resultData")
  private MonthlyServiceResponse oResult;

  @JsonIgnore
  public MonthlyServiceResponseInfoObj setResult(MonthlyServiceResponse oResult) {
    this.oResult = oResult;
    return this;
  }
}

//checkCode	String	驗證資訊,檢查流程使用此數據檢查是否已被綁定
//expiresAt	String	到期時間,驗證資訊到期時間。

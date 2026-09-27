package com.cmc.demo.model.entity.responseinfo.monthly;


import com.cmc.demo.model.entity.monthly.MonthlyServiceEbookGroupResponse;
import com.cmc.demo.model.entity.responseinfo.ResponseInfoObjP;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ResponseObj.java Response Object �q�Ϊ�
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyServiceEbookGroupResponseInfoObj extends ResponseInfoObjP{


  @JsonProperty("resultData")
  private MonthlyServiceEbookGroupResponse oResult;

  @JsonIgnore
  public MonthlyServiceEbookGroupResponseInfoObj setResult(MonthlyServiceEbookGroupResponse oResult) {
    this.oResult = oResult;
    return this;
  }
}

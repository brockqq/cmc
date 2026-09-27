package com.cmc.demo.controller;

import static com.cmc.demo.URLConfig.FUNCTION_MONTHLY_URI;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cmc.demo.annotations.PointOfInterest;
import com.cmc.demo.model.entity.monthly.MonthlyServiceEbookGroupResponse;
import com.cmc.demo.model.entity.response.monthly.MonthlyServiceResponse;
import com.cmc.demo.model.entity.responseinfo.monthly.MonthlyServiceEbookGroupResponseInfoObj;
import com.cmc.demo.model.entity.responseinfo.monthly.MonthlyServiceResponseInfoObj;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;



@PointOfInterest
@Tag(name="4.月租館相關api",description = "4.1.1. 使用者可以月租館頁面看到當前MOMO有的月租包<br>4.1.2. 使用者點選不同的月租包可以看到不同月租包的書單")
@RestController()
public class MonthlyController {
	McokDatas mcokDatas =new McokDatas();
	@PointOfInterest(description="123")
	@Operation(summary = "4.1.1. 使用者可以月租館頁面看到當前MOMO有的月租包，以及對應的書單-取得月租館服務",description = "取得目前月租館清單,沒有登入狀態可以使用")
	@PostMapping(FUNCTION_MONTHLY_URI+"/getMonthlyService")
	public ResponseEntity<MonthlyServiceResponseInfoObj> downloadAppPage(HttpServletResponse response) {
		MonthlyServiceResponseInfoObj r = new MonthlyServiceResponseInfoObj();
		MonthlyServiceResponse rr = new MonthlyServiceResponse();
		rr.setAccunt(mcokDatas.getAccuntAuthInfo());
		rr.setMonthlyServiceDetailList(mcokDatas.getMonthlyServiceDetailList());
		r.setResult(rr);
		return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(r);
	}
	/**
	 * @version 1.0.0
	 * @author brock 
	 */
	@Operation(summary = "4.1.2. 使用者點選不同的月租包可以看到不同月租包的書單-取得特定月租館服務書單",description = "取得特定月租館服務書單,沒有登入狀態可以使用")
	@PostMapping(FUNCTION_MONTHLY_URI+"/monthlyServiceEbooks")
	public ResponseEntity<MonthlyServiceEbookGroupResponseInfoObj> getMonthlyServiceBooks(HttpServletResponse response) {
		
		MonthlyServiceEbookGroupResponseInfoObj mlist = new MonthlyServiceEbookGroupResponseInfoObj();
		
		MonthlyServiceEbookGroupResponse rr = new MonthlyServiceEbookGroupResponse();
		rr.setAccunt(mcokDatas.getAccuntAuthInfo());
		rr.setMonthlyService(mcokDatas.getMonthlyServiceEbookGroup());
		mlist.setResult(rr);
		
		
		return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(mlist);
	}
	
}

package com.cmc.demo.controller.trackExpenses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cmc.demo.annotations.PointOfInterest;
import com.cmc.demo.model.entity.CreditCardInfo;
import com.cmc.demo.model.entity.response.creditcard.CreditCardResponse;
import com.cmc.demo.model.entity.responseinfo.creditcard.CreditCardResponseInfoObj;
import com.cmc.demo.service.trackExpenses.CreditCardService;
import com.cmc.demo.service.trackExpenses.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@PointOfInterest
@Tag(name="信用卡查詢API",description = "提供信用卡查詢相關功能")
@RestController
@RequestMapping ("/payment")
public class PaymentController {
	
	@Autowired
	PaymentService service;
	
	@Autowired
	CreditCardService creditCardService;
	
	
	@GetMapping(value="/version")
	public String version() {
		return "1.0.0";
	}
	
	
	
	@GetMapping(value="/query", produces="application/json;charset=UTF-8")
	public ResponseEntity<Object> query(@RequestParam  String key) {
		List<Object> entities = service.queryByKey(key);
		return new ResponseEntity<Object>(entities, HttpStatus.OK);
	}
	
	/**
	 * 查詢用戶的信用卡列表
	 * @version 1.0.0
	 * @author 
	 */
	@PointOfInterest(description="查詢用戶的信用卡列表")
	@Operation(summary = "查詢用戶的信用卡列表",description = "根據用戶ID查詢該用戶的所有信用卡信息")
	@PostMapping(value="/queryCreditCards", produces="application/json;charset=UTF-8")
	public ResponseEntity<CreditCardResponseInfoObj> queryCreditCards(
			@RequestParam String userId,
			HttpServletResponse response) {
		CreditCardResponseInfoObj result = new CreditCardResponseInfoObj();
		
		try {
			List<CreditCardInfo> creditCards = creditCardService.queryCreditCardsByUserId(userId);
			
			CreditCardResponse creditCardResponse = new CreditCardResponse();
			creditCardResponse.setUserId(userId);
			creditCardResponse.setCreditCards(creditCards);
			
			result.setResult(creditCardResponse);
			return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(result);
		} catch (Exception e) {
			result.setCode(500);
			result.setMessage("查詢信用卡失敗: " + e.getMessage());
			return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(result);
		}
	}
	
	/**
	 * 查詢單張信用卡詳細信息
	 * @version 1.0.0
	 * @author 
	 */
	@PointOfInterest(description="查詢單張信用卡詳細信息")
	@Operation(summary = "查詢單張信用卡詳細信息",description = "根據信用卡ID查詢單張信用卡的詳細信息")
	@PostMapping(value="/getCreditCardDetails", produces="application/json;charset=UTF-8")
	public ResponseEntity<CreditCardResponseInfoObj> getCreditCardDetails(
			@RequestParam String cardId,
			HttpServletResponse response) {
		CreditCardResponseInfoObj result = new CreditCardResponseInfoObj();
		
		try {
			CreditCardInfo creditCard = creditCardService.getCreditCardDetails(cardId);
			if (creditCard != null) {
				List<CreditCardInfo> cards = new ArrayList<>();
				cards.add(creditCard);
				
				CreditCardResponse creditCardResponse = new CreditCardResponse();
				creditCardResponse.setCreditCards(cards);
				
				result.setResult(creditCardResponse);
				return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(result);
			} else {
				result.setCode(404);
				result.setMessage("信用卡不存在");
				return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(result);
			}
		} catch (Exception e) {
			result.setCode(500);
			result.setMessage("查詢信用卡詳情失敗: " + e.getMessage());
			return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(result);
		}
	}
}

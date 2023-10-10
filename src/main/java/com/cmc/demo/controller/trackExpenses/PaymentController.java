package com.cmc.demo.controller.trackExpenses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cmc.demo.service.trackExpenses.PaymentService;

@RestController
@RequestMapping ("/payment")
public class PaymentController {
	
	@Autowired
	PaymentService service;
	
	
	@GetMapping(value="/version")
	public String version() {
		return "1.0.0";
	}
	
	
	
	@GetMapping(value="/query", produces="application/json;charset=UTF-8")
	public ResponseEntity<Object> query(@RequestParam  String key) {
		List<Object> entities = service.queryByKey(key);
		return new ResponseEntity<Object>(entities, HttpStatus.OK);
	}
}

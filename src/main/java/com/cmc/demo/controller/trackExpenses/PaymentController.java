package com.cmc.demo.controller.trackExpenses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping ("/payment")
public class PaymentController {
	@GetMapping(value="/version")
	public String version() {
		return "1.0.0";
	}
	
	@GetMapping(value="/query", produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> query(@RequestParam  String key) {
		List<Object> entities = new ArrayList<Object>();
		Map<String, String> entity = new HashMap<String, String>();
        entity.put("queryKey", key);
        entities.add(entity);
		return new ResponseEntity<Object>(entities, HttpStatus.OK);
	}
}

package com.cmc.demo.controller.trackExpenses;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping ("/payment")
public class PaymentController {
	@GetMapping(value="/version")
	public String version() {
		return "1.0.0";
	}
}

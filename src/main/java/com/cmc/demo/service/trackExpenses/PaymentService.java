package com.cmc.demo.service.trackExpenses;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cmc.demo.repository.PaymentRepository;

@Component
public class PaymentService {
	public static void main(String[] args) {
		PaymentService PaymentService = new PaymentService();
		PaymentService.queryByKey("123");
	}
	PaymentRepository repository ;
	
	public List<Object> queryByKey(String key){
		repository = new PaymentRepository();
		return repository.queryByKey(key);
	}
	
}

package com.cmc.demo.service.trackExpenses;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cmc.demo.repository.PaymentRepository;

@Component
public class PaymentService {

	@Autowired
	PaymentRepository repository ;
	
	public List<Object> queryByKey(String key){
		return repository.queryByKey(key);
	}
	
}

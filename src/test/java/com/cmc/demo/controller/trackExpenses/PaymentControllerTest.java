package com.cmc.demo.controller.trackExpenses;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import com.cmc.demo.CmcApplication;

@ExtendWith(SpringExtension.class) // 原本的 @RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = CmcApplication.class)
class PaymentControllerTest {

	@Autowired
    private WebApplicationContext webApplicationContext;	
	
	
	MockMvc mvc; //創建MockMvc類的物件
	@BeforeEach
	public  void setup(){
//		Memberaccount memberaccount = new Memberaccount();
//		memberaccount.setId(1);
		mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}
	@Test
	public void testVersion() throws Exception {
		String uri = "/payment/version";
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get(uri).accept(MediaType.APPLICATION_JSON)).andReturn();
		int status = result.getResponse().getStatus();
		System.out.println(result.getResponse().getContentAsString());
		assertThat(status).isEqualTo(200);		
	}
	@Test
	public void testQueryn() throws Exception {
		String uri = "/payment/query?key=20231010";
		MvcResult result = mvc.perform(MockMvcRequestBuilders.get(uri).accept(MediaType.APPLICATION_JSON)).andReturn();
		int status = result.getResponse().getStatus();
		System.out.println(result.getResponse().getContentAsString());
		assertThat(status).isEqualTo(200);		
	}
	
}

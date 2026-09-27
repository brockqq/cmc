package com.cmc.demo.controller.trackExpenses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import com.cmc.demo.CmcApplication;
import com.cmc.demo.service.trackExpenses.BookRepository;
import com.cmc.demo.service.trackExpenses.BookRepositoryImpl;
@ExtendWith(SpringExtension.class) // 原本的 @RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = CmcApplication.class)
class PaymentControllerTest {
	@Autowired
	BookRepositoryImpl BookRepository;
	
  @Test
  public void findOneTest() throws Exception{
      BookRepository.findBooksByCustomCriteria2("123");
  }
//
//	@MockBean
//	PaymentRepository repository;
//	
//    @Test
//    public void saveTest() throws Exception {
//        User user = new User();
//        user.setName("郑龙飞");
//        user.setUrl("http://merryyou.cn");
//        User result = userRepository.save(user);
//      //  log.info(result.toString());
//       // Assert.assertNotNull(user.getId());
//    }
//
//    @Test
//    public void findOneTest() throws Exception{
//        Optional<User> user = userRepository.findById(1l);
//        System.out.println(user.toString());
//    }
//	@Autowired
//    private WebApplicationContext webApplicationContext;	
//	@Autowired
//    private UserRepository userRepository;
//	
//	MockMvc mvc; //創建MockMvc類的物件
//	@BeforeEach
//	public  void setup(){
////		Memberaccount memberaccount = new Memberaccount();
////		memberaccount.setId(1);
//		mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
//		List<Object> mockMapList = new LinkedList<Object>();
//			
//		mockMapList.add(new PaymentEntity("202310", "20231010", "水費", new BigDecimal(100)));
//		mockMapList.add(new PaymentEntity("202310", "20231001", "電話費", new BigDecimal(1023)));
//		mockMapList.add(new PaymentEntity("202310", "20231005", "上月結餘", new BigDecimal(3000)));
//		Mockito.when(repository.queryByKey(anyString())).thenReturn(mockMapList);
//	}
//	@Test
//	public void testVersion() throws Exception {
//		String uri = "/payment/version";
//		MvcResult result = mvc.perform(MockMvcRequestBuilders.get(uri).accept(MediaType.APPLICATION_JSON)).andReturn();
//		int status = result.getResponse().getStatus();
//		System.out.println(result.getResponse().getContentAsString());
//		assertThat(status).isEqualTo(200);		
//	}
//	@Test
//	public void testQueryn() throws Exception {
//		String uri = "/payment/query?key=20231010";
//		MvcResult result = mvc.perform(MockMvcRequestBuilders.get(uri).accept(MediaType.APPLICATION_JSON)).andReturn();
//		int status = result.getResponse().getStatus();
//		System.out.println(result.getResponse().getContentAsString());
//		assertThat(status).isEqualTo(200);		
//	}
//	
}

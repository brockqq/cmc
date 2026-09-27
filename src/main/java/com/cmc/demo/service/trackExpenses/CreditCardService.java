package com.cmc.demo.service.trackExpenses;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cmc.demo.model.entity.CreditCardInfo;

/**
 * 信用卡業務服務類
 */
@Component
public class CreditCardService {
	
	/**
	 * 根據用戶ID查詢信用卡列表
	 * @param userId 用戶ID
	 * @return 信用卡列表
	 */
	public List<CreditCardInfo> queryCreditCardsByUserId(String userId) {
		// TODO: 調用外部API獲取信用卡信息
		// 當前返回模擬數據
		return getMockCreditCards(userId);
	}
	
	/**
	 * 獲取模擬信用卡數據（開發用）
	 */
	private List<CreditCardInfo> getMockCreditCards(String userId) {
		List<CreditCardInfo> cards = new ArrayList<>();
		
		CreditCardInfo card1 = new CreditCardInfo();
		card1.setCardId("CARD001");
		card1.setCardNumber("4532xxxxxxxxxxxx");
		card1.setCardHolder("John Doe");
		card1.setExpiryDate("12/26");
		card1.setCardType("VISA");
		card1.setStatus("ACTIVE");
		
		CreditCardInfo card2 = new CreditCardInfo();
		card2.setCardId("CARD002");
		card2.setCardNumber("5412xxxxxxxxxxxx");
		card2.setCardHolder("John Doe");
		card2.setExpiryDate("06/25");
		card2.setCardType("MasterCard");
		card2.setStatus("ACTIVE");
		
		cards.add(card1);
		cards.add(card2);
		
		return cards;
	}
	
	/**
	 * 根據信用卡ID查詢詳細信息
	 * @param cardId 信用卡ID
	 * @return 信用卡詳細信息
	 */
	public CreditCardInfo getCreditCardDetails(String cardId) {
		// TODO: 實現查詢單張信用卡詳細信息的邏輯
		CreditCardInfo card = new CreditCardInfo();
		card.setCardId(cardId);
		card.setCardNumber("4532xxxxxxxxxxxx");
		card.setCardHolder("John Doe");
		card.setExpiryDate("12/26");
		card.setCardType("VISA");
		card.setStatus("ACTIVE");
		return card;
	}
	
	/**
	 * 驗證信用卡是否有效
	 * @param cardId 信用卡ID
	 * @return 驗證結果
	 */
	public boolean validateCreditCard(String cardId) {
		// TODO: 實現信用卡驗證邏輯
		return true;
	}
}


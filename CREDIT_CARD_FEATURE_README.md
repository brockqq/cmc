# 信用卡查詢功能說明文檔 (標準規範版)

## 功能概述
新增了用戶信用卡查詢功能，包括查詢用戶信用卡列表和查詢單張信用卡詳細信息。
本實現完全遵循項目的規範標準，包含 ResponseInfoObjP 統一響應模式、Swagger 文檔註解等。

## API 端點

### 1. 查詢用戶信用卡列表
**端點:** `POST /payment/queryCreditCards`
**參數:** 
- `userId` (String) - 用戶ID (Request Parameter)

**請求示例:**
```bash
curl -X POST "http://localhost:8080/payment/queryCreditCards?userId=user123"
```

**響應示例:**
```json
{
  "success": true,
  "resultCode": 200,
  "resultMessage": "",
  "resultData": {
    "userId": "user123",
    "creditCards": [
      {
        "cardId": "CARD001",
        "cardNumber": "4532xxxxxxxxxxxx",
        "cardHolder": "John Doe",
        "expiryDate": "12/26",
        "cardType": "VISA",
        "status": "ACTIVE"
      },
      {
        "cardId": "CARD002",
        "cardNumber": "5412xxxxxxxxxxxx",
        "cardHolder": "John Doe",
        "expiryDate": "06/25",
        "cardType": "MasterCard",
        "status": "ACTIVE"
      }
    ]
  }
}
```

### 2. 查詢單張信用卡詳細信息
**端點:** `POST /payment/getCreditCardDetails`
**參數:**
- `cardId` (String) - 信用卡ID (Request Parameter)

**請求示例:**
```bash
curl -X POST "http://localhost:8080/payment/getCreditCardDetails?cardId=CARD001"
```

**響應示例:**
```json
{
  "success": true,
  "resultCode": 200,
  "resultMessage": "",
  "resultData": {
    "creditCards": [
      {
        "cardId": "CARD001",
        "cardNumber": "4532xxxxxxxxxxxx",
        "cardHolder": "John Doe",
        "expiryDate": "12/26",
        "cardType": "VISA",
        "status": "ACTIVE"
      }
    ]
  }
}
```

## 項目結構

### 新增文件

#### 1. **Entity 層**
- **CreditCardInfo.java** - 信用卡信息實體
  - 位置：`src/main/java/com/cmc/demo/model/entity/`
  - 包含信用卡的基本信息字段，使用 `@Schema` 註解標記字段

#### 2. **Response 層**
- **CreditCardResponse.java** - 信用卡查詢響應
  - 位置：`src/main/java/com/cmc/demo/model/entity/response/creditcard/`
  - 包含用戶ID和信用卡列表
  
#### 3. **ResponseInfo 層**
- **CreditCardResponseInfoObj.java** - 信用卡查詢響應信息對象
  - 位置：`src/main/java/com/cmc/demo/model/entity/responseinfo/creditcard/`
  - 繼承 `ResponseInfoObjP`，提供統一的響應格式

#### 4. **Service 層**
- **CreditCardService.java** - 信用卡業務服務類
  - 位置：`src/main/java/com/cmc/demo/service/trackExpenses/`
  - 處理信用卡查詢、驗證等業務邏輯
  - 使用 `@Component` 註解標記

#### 5. **Controller 層**
- **PaymentController.java** (更新)
  - 新增信用卡查詢端點
  - 使用 `@PointOfInterest` 和 `@Tag`、`@Operation` 註解進行 Swagger 文檔標記
  - 使用 POST 方式調用，符合項目規範
  - 返回 `CreditCardResponseInfoObj` 統一格式

## 架構設計

項目遵循以下分層架構：

```
Controller 層
    ↓
Service 層 
    ↓
Response 層 (Model)
    ↓
ResponseInfo 層 (統一響應格式)
```

### 響應格式規範

所有 API 響應都使用 `ResponseInfoObjP` 的標準格式：

```json
{
  "success": boolean,      // 是否成功
  "resultCode": integer,   // 結果碼 (200=成功, 404=未找到, 500=錯誤等)
  "resultMessage": string, // 結果消息
  "resultData": object     // 實際數據
}
```

## 集成外部 API 的步驟

### 第1步：選擇外部API服務
常見的支付和信用卡API服務：
- **Stripe** - 功能完整的支付平台
- **Square** - 支付處理平台
- **PayPal** - 知名支付服務
- **Plaid** - 銀行數據API
- **BankConnect** - 銀行連接API

### 第2步：配置 application.yml
編輯 `src/main/resources/application.yml`，添加：

```yaml
credit:
  card:
    api:
      url: "https://api.example.com"
      api-key: "your-api-key"
      api-secret: "your-api-secret"
```

### 第3步：創建 API 客戶端類
在 `src/main/java/com/cmc/demo/client/` 目錄下創建 `CreditCardApiClient.java`：

```java
package com.cmc.demo.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.cmc.demo.model.entity.CreditCardInfo;

@Component
public class CreditCardApiClient {
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Value("${credit.card.api.url:}")
	private String apiUrl;
	
	@Value("${credit.card.api.api-key:}")
	private String apiKey;
	
	public List<CreditCardInfo> queryCreditCardsByUserId(String userId) {
		if (apiUrl == null || apiUrl.isEmpty()) {
			return null; // 返回 null，由 Service 層提供模擬數據
		}
		
		// 實現外部API調用邏輯
		String url = apiUrl + "/api/cards?userId=" + userId;
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + apiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		HttpEntity<String> request = new HttpEntity<>(headers);
		ResponseEntity<List> response = restTemplate.exchange(
			url, 
			HttpMethod.GET, 
			request, 
			List.class
		);
		
		return response.getBody();
	}
}
```

### 第4步：修改 CreditCardService
修改 `CreditCardService.java`，集成 API 客戶端：

```java
@Autowired
private CreditCardApiClient apiClient;

@Override
public List<CreditCardInfo> queryCreditCardsByUserId(String userId) {
	List<CreditCardInfo> cards = apiClient.queryCreditCardsByUserId(userId);
	if (cards != null) {
		return cards;
	}
	// 如果 API 調用失敗或未配置，返回模擬數據
	return getMockCreditCards(userId);
}
```

### 第5步：添加 RestTemplate Bean
創建 `src/main/java/com/cmc/demo/config/RestTemplateConfig.java`：

```java
package com.cmc.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
```

## 當前使用的模擬數據
目前系統使用模擬數據，位於 `CreditCardService.getMockCreditCards()` 方法中。
當集成了外部API後，系統會自動調用外部API而不是返回模擬數據。

## 數據模型

### CreditCardInfo
```
- cardId (String): 信用卡ID
- cardNumber (String): 卡號（應加密存儲）
- cardHolder (String): 持卡人名字
- expiryDate (String): 過期日期 (MM/YY format)
- cardType (String): 卡類型 (VISA, MasterCard等)
- status (String): 狀態 (ACTIVE, INACTIVE, EXPIRED)
```

### CreditCardResponse
```
- userId (String): 用戶ID
- creditCards (List<CreditCardInfo>): 信用卡列表
```

## 安全注意事項
1. **卡號加密**: 不應該在日誌中打印完整卡號，應使用 `xxxx` 掩蓋
2. **API密鑰**: 不應該硬編碼，使用環境變量或配置文件管理
3. **HTTPS**: 調用外部API時必須使用HTTPS
4. **速率限制**: 考慮添加API調用的限流機制
5. **數據驗證**: 驗證所有來自外部API的數據
6. **敏感信息**: 不在日誌中記錄敏感的信用卡信息

## 測試

運行項目測試：
```bash
./mvnw test
```

運行項目：
```bash
./mvnw spring-boot:run
```

訪問 Swagger 文檔：
```
http://localhost:8080/swagger-ui.html
```

## 代碼規範遵循

本實現遵循以下項目規範：

✅ **分層架構** - 遵循 Controller → Service → Response/Entity 的標準分層
✅ **統一響應格式** - 所有 API 返回 ResponseInfoObjP 標準格式
✅ **Swagger 文檔** - 使用 @Tag、@Operation、@Schema 註解生成 API 文檔
✅ **註解規範** - 使用 @PointOfInterest、@Component、@Service 等註解
✅ **命名規範** - 遵循項目現有的命名規範（Info、Response、Service 等）
✅ **方法簽名** - 使用 POST 方式，返回 ResponseEntity<ResponseInfoObjP>

## 後續改進建議
1. 添加數據庫持久化層（Repository）
2. 實現信用卡加密存儲
3. 添加複雜的錯誤處理和重試機制
4. 實現緩存機制提高性能
5. 添加審計日誌（Audit Log）
6. 實現異步API調用（使用 @Async）
7. 添加單元測試和集成測試
8. 實現速率限制（Rate Limiting）


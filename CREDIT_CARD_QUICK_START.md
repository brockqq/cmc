# 信用卡查詢功能 - 快速開始指南

## 📋 新增的文件清單

### Entity 層
- `src/main/java/com/cmc/demo/model/entity/CreditCardInfo.java`

### Response 層  
- `src/main/java/com/cmc/demo/model/entity/response/creditcard/CreditCardResponse.java`

### ResponseInfo 層
- `src/main/java/com/cmc/demo/model/entity/responseinfo/creditcard/CreditCardResponseInfoObj.java`

### Service 層
- `src/main/java/com/cmc/demo/service/trackExpenses/CreditCardService.java`

### Controller 層
- `src/main/java/com/cmc/demo/controller/trackExpenses/PaymentController.java` (已更新)

## 🚀 快速測試

### 方式 1：使用 curl 命令

**查詢用戶信用卡列表：**
```bash
curl -X POST "http://localhost:8080/payment/queryCreditCards?userId=user123"
```

**查詢單張信用卡詳情：**
```bash
curl -X POST "http://localhost:8080/payment/getCreditCardDetails?cardId=CARD001"
```

### 方式 2：使用 Postman

1. 打開 Postman
2. 新建 POST 請求
3. URL：`http://localhost:8080/payment/queryCreditCards?userId=user123`
4. 發送請求

### 方式 3：Swagger 文檔

運行應用後訪問：
```
http://localhost:8080/swagger-ui.html
```

在 "信用卡查詢API" 部分即可看到新增的端點

## 📝 響應格式說明

### 成功響應 (200)

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
      }
    ]
  }
}
```

### 未找到響應 (404)

```json
{
  "success": false,
  "resultCode": 404,
  "resultMessage": "信用卡不存在",
  "resultData": null
}
```

### 錯誤響應 (500)

```json
{
  "success": false,
  "resultCode": 500,
  "resultMessage": "查詢信用卡失敗: [error details]",
  "resultData": null
}
```

## 🔌 集成外部 API

當決定使用哪個外部 API 時，按以下步驟操作：

1. **在 pom.xml 中添加依賴**（如需要）
   ```xml
   <!-- 例如：Stripe API -->
   <dependency>
       <groupId>com.stripe</groupId>
       <artifactId>stripe-java</artifactId>
       <version>latest-version</version>
   </dependency>
   ```

2. **配置 application.yml**
   ```yaml
   credit:
     card:
       api:
         url: "https://api.stripe.com"
         api-key: "your-api-key"
   ```

3. **創建 API 客戶端** (src/main/java/com/cmc/demo/client/CreditCardApiClient.java)
   ```java
   @Component
   public class CreditCardApiClient {
       @Autowired
       private RestTemplate restTemplate;
       
       public List<CreditCardInfo> queryCreditCardsByUserId(String userId) {
           // 實現 API 調用邏輯
       }
   }
   ```

4. **在 CreditCardService 中注入 API 客戶端**
   ```java
   @Autowired
   private CreditCardApiClient apiClient;
   ```

## 📊 數據流程圖

```
Controller (PaymentController)
    ↓
    ├─→ queryCreditCards(userId)
    │      ↓
    │   Service (CreditCardService)
    │      ↓
    │   [API Client] → 外部 API
    │      ↓
    │   返回 List<CreditCardInfo>
    │      ↓
    │   Response (CreditCardResponse)
    │      ↓
    └─→ ResponseInfoObj (統一響應)
          ↓
        返回 ResponseEntity<CreditCardResponseInfoObj>
```

## ✅ 規範檢查清單

- ✅ 遵循項目分層架構（Controller → Service → Response → ResponseInfo）
- ✅ 使用 ResponseInfoObjP 統一響應格式
- ✅ 使用 @PointOfInterest 註解標記
- ✅ 使用 @Tag 和 @Operation 生成 Swagger 文檔
- ✅ 使用 @Schema 對實體字段進行文檔標記
- ✅ 使用 POST 方式提交請求
- ✅ 包含適當的錯誤處理
- ✅ 代碼編譯無誤

## 🔐 安全建議

1. **永遠不要在代碼中硬編碼 API 密鑰**
   ```java
   // ❌ 不要這樣做
   String apiKey = "sk_live_xxxxx";
   
   // ✅ 應該使用配置
   @Value("${credit.card.api.api-key}")
   private String apiKey;
   ```

2. **對敏感信息進行加密**
   - 信用卡號應存儲為加密格式
   - 僅在必要時解密

3. **使用 HTTPS 調用外部 API**
   ```java
   String url = "https://api.example.com/cards"; // ✅ HTTPS
   // String url = "http://api.example.com/cards";  // ❌ HTTP 不安全
   ```

4. **驗證所有外部數據**
   ```java
   if (creditCard != null && creditCard.getCardId() != null) {
       // 驗證成功，繼續處理
   }
   ```

## 📚 相關文檔

- 詳細的功能說明請參考：`CREDIT_CARD_FEATURE_README.md`
- 項目規範請參考：`HELP.md` 和 `README.md`

## ❓ 常見問題

### Q: 如何修改模擬數據？
A: 編輯 `CreditCardService.java` 中的 `getMockCreditCards()` 方法

### Q: 如何切換到真實 API？
A: 
1. 創建 `CreditCardApiClient.java`
2. 在 `application.yml` 中配置 API URL
3. 在 `CreditCardService.java` 中注入 API 客戶端並調用

### Q: 如何調試 API 調用？
A: 使用 Postman 或 curl 測試，檢查響應是否符合預期

### Q: 如何處理 API 超時？
A: 在 RestTemplate 配置中設置超時時間
```java
@Bean
public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory = 
        new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(5000); // 5秒
    factory.setReadTimeout(5000);    // 5秒
    return new RestTemplate(factory);
}
```

## 🎯 下一步建議

1. 根據實際需求集成外部 API
2. 添加持久化存儲（Database）
3. 實現數據緩存機制
4. 添加詳細的日誌記錄
5. 編寫單元測試和集成測試
6. 部署到測試環境進行驗證


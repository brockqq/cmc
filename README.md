# Kata 社區

多社區的住戶服務平台：一個平台上可以有很多社區，每個社區各自管理成員、公告、行事曆與零用金。

- **帳號**：註冊、登入（JWT）、修改密碼，用邀請碼加入一個或多個社區
- **權限**：平台與社區兩層角色；每個社區的角色各自獨立
- **公布欄**：全平台公告與各社區公告，可置頂、分頁
- **行事曆**：社區活動，支援每天／每週／每月／每年重複，也可以只修改或取消其中一次
- **零用金**：定額零用金制度，行政委員登錄支出並上傳單據，社區管理員結算撥補，所有異動都留下紀錄

## 目錄

- [技術架構](#技術架構)
- [快速開始](#快速開始)
- [設定](#設定)
- [部署到正式環境](#部署到正式環境)
- [專案結構](#專案結構)
- [功能說明](#功能說明)
  - [帳號與登入](#帳號與登入)
  - [社區與權限](#社區與權限)
  - [公布欄](#公布欄)
  - [行事曆與重複活動](#行事曆與重複活動)
  - [零用金](#零用金)
- [API](#api)
- [錯誤處理](#錯誤處理)
- [安全性](#安全性)
- [同時操作與資料一致性](#同時操作與資料一致性)
- [資料表](#資料表)
- [測試與開發](#測試與開發)
- [已知限制與待辦](#已知限制與待辦)

## 技術架構

| 目錄 | 技術 |
|------|------|
| `backend/` | Spring Boot 4.1、Java 21、Spring Security（OAuth2 Resource Server + JWT HS256）、Spring Data JPA、Bean Validation、H2、Lombok |
| `frontend/` | Vue 3（`<script setup>`）、Vite、Vue Router、Pinia；Vitest + Vue Test Utils、ESLint |

```
瀏覽器 ──> Vite（開發：5173，/api 轉發）──> Spring Boot（8080）──> H2（記憶體資料庫）
                                                   └──> data/uploads（單據檔案）
```

- 前後端分離，前端透過 `/api/*` 呼叫後端；開發時由 Vite 轉發，不需處理 CORS。
- 無狀態驗證：登入取得 JWT，之後每個請求帶 `Authorization: Bearer <token>`。
- 每次請求都從資料庫讀取使用者最新的角色，所以權限變更會立即生效。

## 快速開始

### 需求

- **JDK 21**。若 `JAVA_HOME` 指向其他版本，請先切換，例如 PowerShell：
  `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"`
- **Node.js** 22.18 以上或 24.12 以上（見 `frontend/package.json` 的 `engines`）

### 啟動後端（http://localhost:8080）

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

沒有指定 profile 時以**開發模式**（`dev`）執行：
- 使用開發用的 JWT 密鑰
- 自動建立平台管理員：帳號 `admin`、密碼 `admin12345`
- 開啟 H2 Console

### 啟動前端（http://localhost:5173）

```bash
cd frontend
npm install
npm run dev
```

### 第一次使用

1. 以 `admin` / `admin12345` 登入。
2. 到「社區管理」建立社區，會自動產生 8 碼邀請碼。
3. 用「指派社區管理員」輸入帳號，把某個會員設為該社區的管理員。
4. 把邀請碼或註冊連結 `/register?invite=邀請碼` 分享給住戶。帶邀請碼註冊的人會以住戶身分加入該社區；已有帳號的人可以在「我的社區」輸入邀請碼加入。
5. 社區管理員可以在社區頁把成員設為行政委員，並設定零用金額度。

## 設定

設定集中在 `backend/src/main/resources/application.properties`（開發預設值在 `application-dev.properties`）。常用的都可以用環境變數覆寫：

| 環境變數 | 預設值 | 說明 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | 設成 `dev` 以外的值（例如 `prod`）就會關閉所有開發預設值 |
| `APP_JWT_SECRET` | 開發模式有預設值 | JWT 簽章密鑰，Base64 編碼、至少 256 bits。產生方式：`openssl rand -base64 32`。**非開發模式必填** |
| `APP_ADMIN_USERNAME` | `admin` | 啟動時若不存在就建立的平台管理員帳號 |
| `APP_ADMIN_EMAIL` | `admin@example.com` | 同上，管理員的 Email |
| `APP_ADMIN_PASSWORD` | 開發模式為 `admin12345` | 管理員的初始密碼（8 字元以上、72 位元組以內）。**非開發模式下，管理員帳號還不存在時必填** |
| `APP_UPLOAD_DIR` | `./data/uploads` | 單據檔案存放目錄（相對於後端的執行目錄） |

其他設定（直接改 properties，或用 `--屬性=值` 啟動參數）：

| 屬性 | 預設值 | 說明 |
|---|---|---|
| `app.jwt.expiration` | `PT2H` | JWT 有效期限（ISO-8601 期間） |
| `app.default-time-zone` | `Asia/Taipei` | 前端沒帶時區時，重複活動使用的時區 |
| `spring.servlet.multipart.max-file-size` | `5MB` | 單據檔案大小上限 |
| `app.rate-limit.login-window` | `PT15M` | 登入失敗的計算區間 |
| `app.rate-limit.login-failures-per-account-and-ip` | `5` | 同一帳號從同一 IP 可失敗的次數 |
| `app.rate-limit.login-failures-per-ip` | `20` | 同一 IP（不分帳號）可失敗的次數 |
| `app.rate-limit.login-failures-per-account` | `50` | 同一帳號（所有 IP 合計）可失敗的次數 |
| `app.rate-limit.registration-window` | `PT1H` | 註冊次數的計算區間 |
| `app.rate-limit.registrations-per-ip` | `10` | 同一 IP 可註冊的次數 |

非開發模式下若缺少 JWT 密鑰或密鑰太短，或需要建立管理員卻沒有提供密碼，**伺服器會拒絕啟動**，並在錯誤訊息說明缺了什麼。這是刻意的：可被猜到的密鑰等於讓任何人都能偽造管理員 token。

## 部署到正式環境

```bash
# 前端：產出靜態檔案到 frontend/dist
cd frontend && npm ci && npm run build

# 後端：產出可執行 jar
cd backend && ./mvnw -DskipTests package
SPRING_PROFILES_ACTIVE=prod \
APP_JWT_SECRET="<固定保存的 Base64 密鑰>" \
APP_ADMIN_PASSWORD='<強密碼>' \
APP_UPLOAD_DIR=/var/lib/kata/uploads \
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

- `frontend/dist` 由網頁伺服器（例如 Nginx）提供，並把 `/api/` 反向代理到後端 8080。因為使用 HTML5 history 路由，未知路徑要回傳 `index.html`。
- JWT 密鑰要固定保存（例如放在密鑰管理服務）。每次重啟都換新密鑰的話，所有人都會被登出。
- 放在反向代理後面時，請設定 `server.forward-headers-strategy=native`（或 `framework`），並讓代理送出 `X-Forwarded-For`。否則後端看到的用戶端 IP 全都是代理的，登入頻率限制會把所有人一起擋掉。
- 正式建置的 `index.html` 已帶 Content-Security-Policy（見[安全性](#安全性)）。若網頁伺服器另外送 CSP header，兩者會同時生效，請保持一致。
- **目前資料庫是 H2 記憶體資料庫**，重啟後資料全部清空，單據檔案卻會留在硬碟上。正式上線前需要換成 PostgreSQL 等持久化資料庫（見[已知限制與待辦](#已知限制與待辦)）。

## 專案結構

```
backend/src/main/java/com/kata/backend/
├── auth/          註冊、登入、JWT 簽發、登入頻率限制（AuthRateLimiter）
├── user/          使用者、修改密碼
├── admin/         平台管理員 API（會員、社區）
├── community/     社區、成員、邀請碼；CommunityAccess（社區權限檢查的唯一入口）
├── announcement/  公布欄
├── event/         行事曆；RecurrenceExpander（重複活動展開）
├── pettycash/     零用金、支出、單據檔案（ReceiptStorage）、異動紀錄（PettyCashLog）
├── config/        Spring Security、JWT、初始管理員
└── common/        錯誤處理（GlobalExceptionHandler）、共用例外與驗證

frontend/src/
├── api/http.js    所有 API 呼叫（自動帶 token；401 時登出）
├── stores/        Pinia：登入狀態（auth）、我的社區（communities）
├── router/        路由與登入／管理員權限守衛
├── views/         頁面：首頁、公布欄、行事曆、零用金、我的社區、社區頁、管理頁、登入／註冊
├── components/    表單與區塊：EventForm、MonthGrid、EventDetail、ExpenseForm、PettyCashLog、ReceiptPreview…
├── composables/   資料載入：useCalendarEvents、usePettyCash
└── utils/         純函式：日期、月曆格子、重複規則說明、金額格式、過期回應保護（latest.js）
```

新增社區範圍的功能（例如報修）時，在 service 開頭呼叫 `CommunityAccess.requireMember` / `requireManager` / `requireAnyRole` 即可，權限規則與「非成員回 404」都會一致。

## 功能說明

### 帳號與登入

- 帳號 3–50 字元，只能用英文字母、數字、底線。**保留註冊時的大小寫，但比對不分大小寫**：有 `Admin` 就不能再註冊 `admin`，登入時打 `ADMIN` 也可以。
- Email 一律存成小寫，不能重複。
- 密碼至少 8 字元、最多 72 **位元組**（BCrypt 的上限；中文字每字佔 3 位元組，所以中文密碼最多 24 字）。
- 修改密碼後，所有已發出的 token 立即失效（JWT 內含 token 版本號 `ver`），包括可能被盜用的 token。
- 登入失敗太多次會被暫時擋下（回 429，附 `Retry-After`），規則見[設定](#設定)。被擋的只有「這個帳號從這個 IP」，本人從別的地方仍能登入。
- 重新整理頁面時，只有 token 失效（401）才會被登出；網路或伺服器暫時出錯不會登出。

### 社區與權限

一個帳號可以加入多個社區，每個社區的角色各自獨立（`community_members` 表）。

| 層級 | 角色 | 可以做的事 |
|------|------|-----------|
| 平台 | `ADMIN` 平台管理員 | 建立社區、指派社區管理員、管理所有會員、發布全平台公告；視同所有社區的管理員與行政委員（不需加入） |
| 平台 | `USER` 一般會員 | 依所屬社區的角色 |
| 社區 | `MANAGER` 社區管理員 | 查看／重新產生邀請碼；管理成員與角色（含指派行政委員、新增管理員）；發布社區公告；管理行事曆；設定零用金額度、結算撥補 |
| 社區 | `COMMITTEE` 行政委員 | 登錄零用金支出、上傳單據；查看零用金帳目 |
| 社區 | `RESIDENT` 住戶 | 查看社區資訊、公告與行事曆 |

- **社區管理員不能變更或移除其他社區管理員**，只有平台管理員可以。這樣一個被盜或心懷不軌的管理員帳號，就無法把其他管理員踢掉、接管整個社區。
- 任何人都不能變更或移除自己（平台管理員也不能改自己的平台角色），避免不小心把自己鎖在外面。
- 邀請碼為 8 碼（不含容易混淆的 0/O、1/I/L），輸入時不分大小寫。重新產生後舊碼立即失效。
- 零用金採**職責分離**：行政委員不能設額度或撥補，社區管理員不能登錄支出；平台管理員兩者皆可。住戶完全看不到零用金。
- 對**不是成員**的人，其他社區看起來就像不存在（一律回 404）；是成員但角色不夠，才回 403。

### 公布欄

- 預設顯示「全平台公告 ＋ 我加入的社區的公告」，也可以只看全平台或只看某個社區。
- 置頂優先，其餘由新到舊；分頁，前端可選每頁 5／10／20／50 筆（API 上限 100），頁碼記在網址裡。
- 全平台公告只有平台管理員能發布與修改；社區公告由該社區管理員（或平台管理員）管理；住戶唯讀。
- 發布後不能改發布對象（社區）。標題最多 200 字、內容最多 5000 字。

### 行事曆與重複活動

- 活動一律屬於某個社區，由該社區管理員管理；成員只看得到自己社區的活動。
- 月曆畫面每天最多顯示兩個活動；手機寬度改顯示圓點。日期格可以用鍵盤選取（Tab 移動、Enter 選取）。下方列出接下來 90 天的活動。
- 活動時間需在 2000–2099 年之間，以秒為單位儲存。

`recurrence` 範例（每週二、四，共 10 次）：

```json
{ "frequency": "WEEKLY", "interval": 1, "daysOfWeek": ["TUESDAY", "THURSDAY"], "count": 10 }
```

- `frequency`：`DAILY` / `WEEKLY` / `MONTHLY` / `YEARLY`；`interval`：每 N 個單位（1–99）。
- `daysOfWeek`：只用於每週重複，省略時為開始日當天的星期。**開始日一定是第一場**（也計入 `count`），即使它不在勾選的星期裡，與 Google 日曆、Outlook 相同。
- 結束條件：`until`（日期，含當天）或 `count`（1–500 次）擇一；都不給表示永不結束。`until` 最多為開始後 10 年。
- 依活動的時區計算：固定每週一 19:00 當地時間，跨日光節約時間也不會偏移。每月 31 日、2/29 等不存在的日期，落在該月最後一天。

**單一場次的操作**

- 每一場以 `originalStart`（系列原本排定的開始時間）識別，列表回傳的每一場都帶有 `originalStart` 與 `modified`。
- **只改這一次**：可以改時間（可移到任何日期）、標題、地點、說明。與系列相同的欄位會繼續跟隨系列（例如之後改了系列標題，這一場也會更新）；地點或說明清空時，這一場就是「沒有」，不會回退成系列的值。
- **還原為系列設定**：清除這一場的個別調整。
- **取消這一次**：其他場次不受影響；取消一個已調整過的場次，會一併移除它的調整。

**編輯整個系列時，已取消或已調整的場次如何處理**

| 系列的變更 | 取消與個別調整 |
|---|---|
| 只改標題、地點、說明 | 全部保留 |
| 只改時段（開始日期與重複規則不變，例如 19:00 改成 20:30） | 跟著移到新時段：取消的場次仍是取消；只改標題的場次跟著新時間；另外指定過時間的場次維持原本的時間 |
| 開始日期或重複規則改變 | 已不存在的場次上的取消與調整會清除，其餘保留 |

### 零用金

每個社區一個定額零用金（imprest fund），金額以新台幣「元」為單位的整數：

1. 社區管理員設定**每期額度**（例：5,000），第 1 期以此金額開始。
2. 行政委員收到單據後**登錄支出**：日期、金額、用途，以及選填的付款對象、單據號碼、備註，可附單據照片或 PDF（JPG／PNG／GIF／WebP／PDF，5MB 內）。餘額可以是負數（超支）。
3. 社區管理員**結算並撥補**：本期結束，下一期撥入「額度 − 餘額」，餘額回到額度，超支的部分一併補回。
   - 例：額度 5,000，本期支出 5,700 → 餘額 −700 → 撥補 5,700 → 第 2 期餘額 5,000。
   - 調整額度會在下次撥補時生效；若餘額高於新額度，撥補金額為負數（繳回）。
   - 本期沒有任何支出、餘額也等於額度時，不需要結算（按鈕停用，API 回 400）。
4. **已結算的期別不可修改**，只有本期的支出可以編輯或刪除。
5. **異動紀錄**：每一筆登錄、修改、刪除、單據上傳／更換／移除、額度調整與撥補，都會記下誰、何時、哪些欄位從什麼改成什麼（例：「金額：NT$ 1,200 → NT$ 1,500」）。紀錄只能新增，不能修改或刪除；支出刪除後，仍能從紀錄查到原本的內容。支出明細也會顯示「最後由誰修改」。

單據檔案存在 `APP_UPLOAD_DIR`，使用隨機檔名，以檔案開頭的位元組判斷真實格式（不相信副檔名或瀏覽器宣稱的類型）。下載需要登入且有查看權限；交易失敗時，已寫入的檔案會被清掉。

## API

- 除了註冊與登入，所有 API 都需要 `Authorization: Bearer <token>`。
- 請求與回應為 JSON；時間為 ISO-8601（例：`2026-10-01T11:00:00Z`），日期為 `yyyy-MM-dd`。
- 「社區成員」「社區管理員」等權限，平台管理員一律通過。

### 帳號

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| POST | `/api/auth/register` | — | 註冊 `{username, email, password, inviteCode?}` → 201。帶邀請碼會同時以住戶身分加入社區；邀請碼無效時整筆註冊不會成立 |
| POST | `/api/auth/login` | — | 登入 `{username, password}` → `{token, expiresIn, user}` |
| GET | `/api/users/me` | 登入 | 目前登入者資料 |
| PUT | `/api/users/me/password` | 登入 | 修改密碼 `{currentPassword, newPassword}`；成功後所有既有 token 失效 |

### 社區

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/api/communities/mine` | 登入 | 我加入的社區與我在各社區的角色 |
| POST | `/api/communities/join` | 登入 | 用邀請碼加入 `{inviteCode}`（不分大小寫）→ 201 |
| GET | `/api/communities/{id}` | 社區成員 | 社區資訊；管理者才會拿到 `inviteCode` |
| GET | `/api/communities/{id}/members` | 社區管理員 | 成員列表 |
| PUT | `/api/communities/{id}/members/{userId}/role` | 社區管理員 | 變更成員角色 `{role: "MANAGER" \| "COMMITTEE" \| "RESIDENT"}`（不能改自己；不能改其他管理員，平台管理員除外） |
| DELETE | `/api/communities/{id}/members/{userId}` | 社區管理員 | 移除成員（同上限制）→ 204 |
| POST | `/api/communities/{id}/invite-code` | 社區管理員 | 重新產生邀請碼，舊碼立即失效 |

### 平台管理

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/api/admin/users` | `ADMIN` | 列出所有會員 |
| PUT | `/api/admin/users/{id}/role` | `ADMIN` | 變更平台角色 `{role: "USER" \| "ADMIN"}`（不能改自己） |
| GET | `/api/admin/communities` | `ADMIN` | 列出所有社區（含邀請碼、成員數） |
| POST | `/api/admin/communities` | `ADMIN` | 建立社區 `{name, description?}` → 201（名稱不可重複） |
| POST | `/api/admin/communities/{id}/managers` | `ADMIN` | 指派社區管理員 `{username}`（帳號不分大小寫；尚未加入的會自動加入） |

### 公布欄

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/api/announcements?communityId=&platform=&page=&size=` | 登入 | 分頁列表。預設為全平台＋我加入社區的公告；`platform=true` 只看全平台；`communityId` 只看該社區（需為成員）。`page` 從 0 起、`size` 1–100（預設 10）。回傳 `{items, page, size, totalItems, totalPages}` |
| POST | `/api/announcements` | 見說明 | 發布 `{communityId, title, content, pinned?}`；`communityId: null` 為全平台公告，僅平台管理員 → 201 |
| PUT | `/api/announcements/{id}` | 見說明 | 編輯 `{title, content, pinned?}`（不能改發布對象） |
| DELETE | `/api/announcements/{id}` | 見說明 | 刪除 → 204 |

### 行事曆

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/api/events?communityId=&from=&to=` | 登入 | 與 [from, to) 重疊的**場次**（重複活動會展開成每一次），依開始時間排序。預設為現在起 90 天，區間最長 400 天；不帶 `communityId` 為我加入的所有社區 |
| GET | `/api/events/{id}` | 社區成員 | 活動或系列本身（系列層級的設定，給「編輯系列」表單用） |
| POST | `/api/events` | 社區管理員 | 新增 `{communityId, title, startAt, endAt?, location?, description?, timeZone?, recurrence?}` → 201。`timeZone` 為 IANA 時區，預設 `Asia/Taipei` |
| PUT | `/api/events/{id}` | 社區管理員 | 編輯活動；重複活動為整個系列（不能改社區）。body 可帶 `version` |
| DELETE | `/api/events/{id}?version=` | 社區管理員 | 刪除活動或整個系列 → 204 |
| PUT | `/api/events/{id}/occurrences?start=` | 社區管理員 | 只修改某一次 `{title, startAt, endAt?, location?, description?, version?}` |
| DELETE | `/api/events/{id}/occurrences/changes?start=&version=` | 社區管理員 | 將某一次還原為系列設定 |
| DELETE | `/api/events/{id}/occurrences?start=&version=` | 社區管理員 | 取消重複活動中的某一次 → 204 |

- `start` 為該場的 `originalStart`。
- `version` 皆為選填，建議一律帶上，說明見[同時操作與資料一致性](#同時操作與資料一致性)。

每一場的回應欄位：

| 欄位 | 說明 |
|---|---|
| `startAt` / `endAt` | 這一場實際的時間 |
| `originalStart` | 系列原本排定的時間，單場操作用它識別；只有個別調整過的場次才會與 `startAt` 不同 |
| `modified` | 是否個別調整過 |
| `seriesStartAt` / `seriesEndAt` | 系列第一場的時間（編輯系列用） |
| `recurrence`、`timeZone` | 重複規則與時區（單次活動的 `recurrence` 為 null） |
| `canEdit` | 目前使用者能否管理 |
| `version` | 活動的版本號 |

### 零用金

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/api/communities/{id}/petty-cash` | 管理員／行政委員 | 總覽：額度、餘額、下次撥補金額、本期與歷史期別（每期的撥補、支出合計、期末餘額、開始與結算人） |
| PUT | `/api/communities/{id}/petty-cash/fund` | 社區管理員 | 設定每期額度 `{amount}`；第一次設定會開始第 1 期 |
| POST | `/api/communities/{id}/petty-cash/replenish` | 社區管理員 | 結算本期並撥補 `{note?}`；撥補金額為額度減餘額 |
| GET | `/api/communities/{id}/petty-cash/expenses?period=` | 管理員／行政委員 | 某一期的支出（預設本期）；修改過的支出帶有 `updatedBy` / `updatedAt` |
| GET | `/api/communities/{id}/petty-cash/log?period=` | 管理員／行政委員 | 某一期的異動紀錄（新到舊），含已刪除的支出。每筆為 `{action, expenseId, subject, changes[], actor, at}` |
| POST | `/api/communities/{id}/petty-cash/expenses` | 行政委員 | 登錄支出 `{spentOn, amount, purpose, payee?, receiptNo?, note?}` → 201 |
| PUT | `/api/petty-cash/expenses/{id}` | 行政委員 | 修改支出（僅限本期） |
| DELETE | `/api/petty-cash/expenses/{id}` | 行政委員 | 刪除支出（僅限本期）→ 204 |
| POST | `/api/petty-cash/expenses/{id}/attachment` | 行政委員 | 上傳或更換單據，multipart 欄位 `file` |
| GET | `/api/petty-cash/expenses/{id}/attachment` | 管理員／行政委員 | 下載單據 |
| DELETE | `/api/petty-cash/expenses/{id}/attachment` | 行政委員 | 移除單據 |

異動紀錄的 `action`：`EXPENSE_CREATED`、`EXPENSE_UPDATED`、`EXPENSE_DELETED`、`ATTACHMENT_ADDED`、`ATTACHMENT_REPLACED`、`ATTACHMENT_REMOVED`、`FUND_SET`、`REPLENISHED`。

## 錯誤處理

錯誤回應為 [RFC 9457 Problem Detail](https://www.rfc-editor.org/rfc/rfc9457)：`detail` 是可以直接顯示給使用者的中文訊息，欄位驗證錯誤放在 `errors`（欄位名 → 訊息）。

```json
{ "status": 400, "detail": "輸入資料格式錯誤", "errors": { "password": "密碼至少需要 8 個字元" } }
```

| 狀態碼 | 意義 |
|---|---|
| 400 | 輸入不正確（驗證失敗、邀請碼無效、不合理的時間、不需要的撥補…） |
| 401 | 沒有 token、token 過期或已失效，或帳號密碼錯誤 |
| 403 | 是社區成員，但角色不足 |
| 404 | 找不到；**也包括不是成員的社區**，看起來與不存在相同 |
| 409 | 資料已存在，或剛被別人改過：帳號／Email 已被使用、已是社區成員、活動的 `version` 已過期、已結算的期別不能修改，或兩個請求同時新增同一筆資料 |
| 413 | 上傳檔案超過 5MB |
| 429 | 嘗試次數過多，`Retry-After` 表示幾秒後可以再試 |

## 安全性

- **密鑰**：非開發模式必須提供 JWT 密鑰與管理員初始密碼，否則拒絕啟動；H2 Console 只在開發模式開啟。
- **密碼**：以 BCrypt 雜湊；限制 72 位元組，避免超過 BCrypt 上限造成錯誤。
- **帳號探測**：登入時不論帳號是否存在，都做一次成本相同的密碼比對，無法從回應時間判斷。註冊時仍會告知帳號或 Email 已被使用（這是註冊本身必要的資訊），並受頻率限制保護。
- **社區探測**：非成員一律回 404，無法用 id 猜出有哪些社區。
- **權限即時生效**：每次請求都從資料庫讀取最新角色；修改密碼會讓舊 token 立即失效。
- **單據檔案**：以檔案內容判斷真實格式，只接受圖片與 PDF，隨機檔名並防止路徑穿越。下載回應帶 `Cache-Control: private, no-store`、`X-Content-Type-Options: nosniff` 與限制性的 CSP。前端只預覽允許的類型。
- **前端 CSP**：正式建置的 `index.html` 帶 Content-Security-Policy，只允許載入自己的程式與樣式，單據預覽只允許頁面自己產生的 `blob:`。`object-src` 設為 `blob:` 而不是 `'none'`，因為 Chrome 把 iframe 裡的 PDF 當作外掛內容，設成 `'none'` 可能會讓 PDF 單據無法顯示。開發伺服器不套用 CSP（其工具會注入 inline script）。
- **頻率限制**：見[帳號與登入](#帳號與登入)。紀錄存在記憶體，只對單一伺服器有效。

## 同時操作與資料一致性

多人同時操作時的處理方式：

- **零用金**：登錄、修改、刪除支出、上傳單據與結算撥補，都會先鎖住該社區的零用金，依序執行。所以撥補的同時有人在修改支出，修改的一方會等撥補完成，然後看到期別已結算（409），不會改到已結算的帳。
- **行事曆**：同一個活動的所有修改（系列與單場）都會鎖住該活動，依序執行，不會留下幽靈場次。
- **活動版本**：每個活動都有 `version`，系列或任何一場改變時都會增加。修改時帶上看到的 `version`，若期間有人改過就回 409，不會默默覆蓋對方的修改。前端會自動重新載入，並在表單中提示；已輸入的內容保留，重新開啟表單即可看到最新資料。
- **唯一性**：帳號（不分大小寫，由 `username_key` 欄位保證）、Email、社區名稱、社區成員、每社區一個零用金等，都有資料庫的唯一限制。兩個請求同時搶同一筆時，其中一個會拿到 409 而不是 500。
- **前端**：快速切換社區、月份或分頁時，較舊的回應不會蓋掉較新的結果（`utils/latest.js`）。零用金頁切換社區時會先隱藏數字與按鈕，操作一律針對畫面上顯示的社區。

## 資料表

由 JPA 在啟動時建立（`ddl-auto=create-drop`）：

| 資料表 | 內容 |
|---|---|
| `users` | 會員；`username_key` 為小寫帳號（唯一），`token_version` 用來讓舊 token 失效 |
| `communities` | 社區與邀請碼 |
| `community_members` | 成員與社區角色（社區＋會員唯一） |
| `announcements` | 公告；`community_id` 為 null 表示全平台 |
| `calendar_events` | 活動或系列（重複規則為內嵌欄位）；`last_end_at` 為系列最後結束時間，永不結束為 null；`version` 為版本號 |
| `calendar_event_exclusions` | 取消的場次 |
| `calendar_event_overrides` | 個別調整的場次（活動＋`original_start` 唯一） |
| `petty_cash_funds` | 每個社區的零用金與每期額度（每社區唯一） |
| `petty_cash_periods` | 期別：撥補金額、開始與結算時間及人員 |
| `petty_cash_expenses` | 支出與單據檔案資訊 |
| `petty_cash_logs` | 零用金異動紀錄（只新增） |

## 測試與開發

```bash
# 後端（JDK 21）：整合測試 + 單元測試
cd backend && ./mvnw test

# 前端
cd frontend
npm test          # Vitest（單次執行）；npm run test:watch 為監看模式
npm run lint      # ESLint
npm run build     # 正式建置
```

- 後端測試以 MockMvc 呼叫真實 API，涵蓋權限、重複活動展開（含「跳過前段」與完整展開的等價測試）、零用金流程、並發情境（撥補與修改同時、系列與單場同時、兩人同時註冊同一帳號）與正式環境設定。
- 測試專用的設定在 `backend/src/test/resources/config/application.properties`（放寬每 IP 的頻率限制，因為所有測試請求都來自 127.0.0.1）。
- 前端測試涵蓋日期與月曆工具、重複規則說明、金額格式，以及零用金頁「快速切換社區時舊回應不會蓋掉新結果」。
- H2 Console（僅開發模式）：http://localhost:8080/h2-console，JDBC URL `jdbc:h2:mem:katadb`，帳號 `sa`，無密碼。資料存在記憶體，重啟即清空。

## 已知限制與待辦

- **資料庫**：目前是 H2 記憶體資料庫，重啟即清空，資料表由 JPA 自動建立。上線前需要改用 PostgreSQL 等持久化資料庫，並以 Flyway 管理資料表版本（包含 `username_key`、各索引與唯一限制）。
- **頻率限制存在記憶體**：重啟即清空，多台伺服器時各算各的；多台部署時可改用 Redis 等共用儲存。
- **單據檔案**存在本機磁碟；多台部署時需改用共用儲存（例如物件儲存服務）。
- 目前沒有 Email 寄送，因此沒有「忘記密碼」與 Email 驗證。

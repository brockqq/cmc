package com.cmc.demo.model.entity.ebook;

import java.math.BigDecimal;
import java.util.ArrayList;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "提供清單頁使用的entity")
public class EBook {
	private String deliverId = "123123";

	private String contentId = "";

	@Schema(type = "String", example = "職業棒球 9月號/2023 第498期", description = "書名")
	private String title = "";

	@Schema(type = "String", example = "中華職業棒球大聯盟", description = "出版社")
	private String publisher = "";
	
	@Schema(type = "String", example = "中華職業棒球大聯盟", description = "作者")
	private String author = "";
	
	@Schema(type = "String", example = "202", description = "")
	private String bodyTypeCode = "";

	private ArrayList<String> categories = new ArrayList<String>();

	@Schema(type = "String", example = "雜誌", description = "主類別名稱")
	private String ebookType = "";

	private String updateDate = "";

	private String purchaseDate = "";

	private String publishDate = "";

	@Schema(type = "String", example = "雜誌", description = "狀態(是否有購買過)")
	private String status = "";

	private String cover = "";

	private String vertical;

	
	@Schema(type = "String", example = "完整收錄「棒球情人」高國輝生涯，無論是國際賽關鍵一擊.......", description = "書籍簡介")
	private String desc;

	private String onDate = "";

	private String deliverDate = "";

	@Schema(type = "String", example = "Default", description = "epub顯示方式(預設Default)")
	private String epubLayout = "";

	private int dpo;

	@Schema(type = "Boolean", example = "felse", description = "是否是成人內容")
	private Boolean isAdult;

	private Boolean isEmbeddedVideo;

	@Schema(type = "int", example = "30", description = "近2週的下載數")
	private int hotDownloads;
	@Schema(type = "Boolean", example = "false", description = "是否提供文字模式，方便可以貼標籤")
	private Boolean isProvideText;

	@Schema(type = "BigDecimal", example = "0", description = "原價")
	private BigDecimal originalPrice;

	@Schema(type = "BigDecimal", example = "0", description = "促銷價")
	private BigDecimal salePrice;

	private Boolean isGift;

	private String giftName = "";

	private String giftPictureUrl = "";

	private String giftDesc = "";

	private Boolean isHdPdf;

	private String goodsCode;

	public String getOnDate() {
		return onDate;
	}

	public void setOnDate(String onDate) {
		this.onDate = onDate;
	}

	public String getDeliverDate() {
		return deliverDate;
	}

	public void setDeliverDate(String deliverDate) {
		this.deliverDate = deliverDate;
	}

	public String getDeliverId() {
		return deliverId;
	}

	public void setDeliverId(String deliverId) {
		this.deliverId = deliverId;
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getBodyTypeCode() {
		return bodyTypeCode;
	}

	public void setBodyTypeCode(String bodyTypeCode) {
		this.bodyTypeCode = bodyTypeCode;
	}

	public String getEbookType() {
		return ebookType;
	}

	public void setEbookType(String ebookType) {
		this.ebookType = ebookType;
	}

	public ArrayList<String> getCategories() {
		return categories;
	}

	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}

	public String getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(String updateDate) {
		this.updateDate = updateDate;
	}

	public String getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(String purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(String publishDate) {
		this.publishDate = publishDate;
	}

	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}

	public String getVertical() {
		return vertical;
	}

	public void setVertical(String vertical) {
		this.vertical = vertical;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getDpo() {
		return dpo;
	}

	public void setDpo(int dpo) {
		this.dpo = dpo;
	}

	public Boolean getIsEmbeddedVideo() {
		return isEmbeddedVideo;
	}

	public void setIsEmbeddedVideo(Boolean isEmbeddedVideo) {
		this.isEmbeddedVideo = isEmbeddedVideo;
	}

	public String getEpubLayout() {
		return epubLayout;
	}

	public void setEpubLayout(String epubLayout) {
		this.epubLayout = epubLayout;
	}

	public void setAdult(Boolean isAdult) {
		this.isAdult = isAdult;
	}

	public Boolean isAdult() {
		return isAdult;
	}

	public int getHotDownloads() {
		return hotDownloads;
	}

	public void setHotDownloads(int hotDownloads) {
		this.hotDownloads = hotDownloads;
	}

	public Boolean getIsProvideText() {
		return isProvideText;
	}

	public void setIsProvideText(Boolean isProvideText) {
		this.isProvideText = isProvideText;
	}

	public BigDecimal getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(BigDecimal originalPrice) {
		this.originalPrice = originalPrice;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(BigDecimal salePrice) {
		this.salePrice = salePrice;
	}

	public void setIsGift(Boolean isGift) {
		this.isGift = isGift;
	}

	public Boolean isGift() {
		return isGift;
	}

	public String getGiftName() {
		return giftName;
	}

	public void setGiftName(String giftName) {
		this.giftName = giftName;
	}

	public String getGiftPictureUrl() {
		return giftPictureUrl;
	}

	public void setGiftPictureUrl(String giftPictureUrl) {
		this.giftPictureUrl = giftPictureUrl;
	}

	public String getGiftDesc() {
		return giftDesc;
	}

	public void setGiftDesc(String giftDesc) {
		this.giftDesc = giftDesc;
	}

	public Boolean getIsHdPdf() {
		return isHdPdf;
	}

	public void setIsHdPdf(Boolean isHdPdf) {
		this.isHdPdf = isHdPdf;
	}

	public String getGoodsCode() {
		return goodsCode;
	}

	public void setGoodsCode(String goodsCode) {
		this.goodsCode = goodsCode;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 59 * hash + (this.deliverId != null ? this.deliverId.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final EBook other = (EBook) obj;
		if ((this.deliverId == null) ? (other.deliverId != null) : !this.deliverId.equals(other.deliverId)) {
			return false;
		}
		return true;
	}
}

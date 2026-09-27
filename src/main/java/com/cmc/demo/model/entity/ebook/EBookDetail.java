package com.cmc.demo.model.entity.ebook;

import java.util.ArrayList;

public class EBookDetail{

    private String deliverId = null;

    private String contentId = null;

    private String title = null;

    private String publisher = null;

    private String author = null;

    private String bodyTypeCode = null;

    private ArrayList<String> categories = null;

    private String ebookType = null;

    private String updateDate = null;

    private String purchaseDate = null;

    private String publishDate = null;

    private String status = null;

    private String cover = null;

    private String vertical;

    private String desc;

    private String onDate = null;

    private String deliverDate = null;

    private String epubLayout = null;

    private int dpo;

    private Boolean isAdult;

    private Boolean isEmbeddedVideo;

    private int hotDownloads;

    private Boolean isProvideText;

    private int originalPrice;

    private int salePrice;

    private Boolean isGift;

    private String giftName = null;

    private String giftPictureUrl = null;

    private String giftDesc = null;

    private Boolean isHdPdf;

    private String goodsCode;

    public String getOnDate(){
        return onDate;
    }

    public void setOnDate(String onDate){
        this.onDate = onDate;
    }

    public String getDeliverDate(){
        return deliverDate;
    }

    public void setDeliverDate(String deliverDate){
        this.deliverDate = deliverDate;
    }

    public String getDeliverId(){
        return deliverId;
    }

    public void setDeliverId(String deliverId){
        this.deliverId = deliverId;
    }

    public String getContentId(){
        return contentId;
    }

    public void setContentId(String contentId){
        this.contentId = contentId;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getPublisher(){
        return publisher;
    }

    public void setPublisher(String publisher){
        this.publisher = publisher;
    }

    public String getAuthor(){
        return author;
    }

    public void setAuthor(String author){
        this.author = author;
    }

    public String getBodyTypeCode(){
        return bodyTypeCode;
    }

    public void setBodyTypeCode(String bodyTypeCode){
        this.bodyTypeCode = bodyTypeCode;
    }

    public String getEbookType(){
        return ebookType;
    }

    public void setEbookType(String ebookType){
        this.ebookType = ebookType;
    }

    public ArrayList<String> getCategories(){
        return categories;
    }

    public void setCategories(ArrayList<String> categories){
        this.categories = categories;
    }

    public String getUpdateDate(){
        return updateDate;
    }

    public void setUpdateDate(String updateDate){
        this.updateDate = updateDate;
    }

    public String getPurchaseDate(){
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate){
        this.purchaseDate = purchaseDate;
    }

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public String getPublishDate(){
        return publishDate;
    }

    public void setPublishDate(String publishDate){
        this.publishDate = publishDate;
    }

    public String getCover(){
        return cover;
    }

    public void setCover(String cover){
        this.cover = cover;
    }

    public String getVertical(){
        return vertical;
    }

    public void setVertical(String vertical){
        this.vertical = vertical;
    }

    public String getDesc(){
        return desc;
    }

    public void setDesc(String desc){
        this.desc = desc;
    }

    public int getDpo(){
        return dpo;
    }

    public void setDpo(int dpo){
        this.dpo = dpo;
    }

    public Boolean getIsEmbeddedVideo(){
        return isEmbeddedVideo;
    }

    public void setIsEmbeddedVideo(Boolean isEmbeddedVideo){
        this.isEmbeddedVideo = isEmbeddedVideo;
    }

    public String getEpubLayout(){
        return epubLayout;
    }

    public void setEpubLayout(String epubLayout){
        this.epubLayout = epubLayout;
    }

    public void setAdult(Boolean isAdult){
        this.isAdult = isAdult;
    }

    public Boolean isAdult(){
        return isAdult;
    }

    public int getHotDownloads(){
        return hotDownloads;
    }

    public void setHotDownloads(int hotDownloads){
        this.hotDownloads = hotDownloads;
    }

    public Boolean getIsProvideText(){
        return isProvideText;
    }

    public void setIsProvideText(Boolean isProvideText){
        this.isProvideText = isProvideText;
    }

    public int getOriginalPrice(){
        return originalPrice;
    }

    public void setOriginalPrice(int originalPrice){
        this.originalPrice = originalPrice;
    }

    public int getSalePrice(){
        return salePrice;
    }

    public void setSalePrice(int salePrice){
        this.salePrice = salePrice;
    }

    public void setIsGift(Boolean isGift){
        this.isGift = isGift;
    }

    public Boolean isGift(){
        return isGift;
    }

    public String getGiftName(){
        return giftName;
    }

    public void setGiftName(String giftName){
        this.giftName = giftName;
    }

    public String getGiftPictureUrl(){
        return giftPictureUrl;
    }

    public void setGiftPictureUrl(String giftPictureUrl){
        this.giftPictureUrl = giftPictureUrl;
    }

    public String getGiftDesc(){
        return giftDesc;
    }

    public void setGiftDesc(String giftDesc){
        this.giftDesc = giftDesc;
    }

    public Boolean getIsHdPdf(){
        return isHdPdf;
    }

    public void setIsHdPdf(Boolean isHdPdf){
        this.isHdPdf = isHdPdf;
    }

    public String getGoodsCode(){
        return goodsCode;
    }

    public void setGoodsCode(String goodsCode){
        this.goodsCode = goodsCode;
    }

    @Override
    public int hashCode(){
        int hash = 5;
        hash = 59 * hash + ( this.deliverId != null ? this.deliverId.hashCode() : 0 );
        return hash;
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null){
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }
        final EBookDetail other = (EBookDetail) obj;
        if (( this.deliverId == null ) ? ( other.deliverId != null ) : !this.deliverId.equals(other.deliverId)){
            return false;
        }
        return true;
    }
}

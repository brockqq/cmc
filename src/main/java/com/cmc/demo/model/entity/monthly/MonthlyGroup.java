package com.cmc.demo.model.entity.monthly;

import java.util.ArrayList;
import java.util.List;

import com.cmc.demo.model.entity.ebook.EBook;

import io.swagger.v3.oas.annotations.media.Schema;

public class MonthlyGroup {
	
	@Schema(description="群組id",type ="String",example = "XXX1100100001")
	private String contentGroupId = "" ;
	
	@Schema(type ="String",example = "職業棒球",description = "外層顯示的名稱")
	private String groupName = "";
	private Integer dpo = 0; 
	
	@Schema(type ="String",example = "/images/COVER/XXX_180.jpg,/images/COVER/XXX_180.jpg,/images/COVER/XXX_180.jpg",description = "圖檔位置,多張圖用「,」隔開")
	private String pic= "";	
	private String lastDeliverDate = "";
	private String lastPublishDate = "";
	@Schema(type ="List<EBook>",example = "",description = "此群組下書單")
	private List<EBook> ebooks =new ArrayList<EBook>();
	public String getContentGroupId() {
		return contentGroupId;
	}
	public void setContentGroupId(String contentGroupId) {
		this.contentGroupId = contentGroupId;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public Integer getDpo() {
		return dpo;
	}
	public void setDpo(Integer dpo) {
		this.dpo = dpo;
	}
	public String getPic() {
		return pic;
	}
	public void setPic(String pic) {
		this.pic = pic;
	}
	public String getLastDeliverDate() {
		return lastDeliverDate;
	}
	public void setLastDeliverDate(String lastDeliverDate) {
		this.lastDeliverDate = lastDeliverDate;
	}
	public String getLastPublishDate() {
		return lastPublishDate;
	}
	public void setLastPublishDate(String lastPublishDate) {
		this.lastPublishDate = lastPublishDate;
	}
	public List<EBook> getEbooks() {
		return ebooks;
	}
	public void setEbooks(List<EBook> ebooks) {
		this.ebooks = ebooks;
	}

	
	
	
}

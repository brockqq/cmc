package com.cmc.demo.controller;



import java.util.ArrayList;
import java.util.List;

import com.cmc.demo.model.entity.AccuntAuthInfo;
import com.cmc.demo.model.entity.monthly.MonthlyGroup;
import com.cmc.demo.model.entity.monthly.MonthlyServiceDetail;
import com.cmc.demo.model.entity.monthly.MonthlyServiceEbookGroup;



public class McokDatas {
	
	
	private  AccuntAuthInfo accuntAuthInfo = new AccuntAuthInfo();
	private List<MonthlyServiceDetail> monthlyServiceDetailList =new ArrayList<MonthlyServiceDetail>();
	private MonthlyServiceEbookGroup monthlyServiceEbookGroup = new MonthlyServiceEbookGroup();

	public AccuntAuthInfo getAccuntAuthInfo() {
		return accuntAuthInfo;
	}

	public void setAccuntAuthInfo(AccuntAuthInfo accuntAuthInfo) {
		this.accuntAuthInfo = accuntAuthInfo;
	}

	public List<MonthlyServiceDetail> getMonthlyServiceDetailList() {
		return monthlyServiceDetailList;
	}

	public void setMonthlyServiceDetailList(List<MonthlyServiceDetail> monthlyServiceDetailList) {
		this.monthlyServiceDetailList = monthlyServiceDetailList;
	}

	public MonthlyServiceEbookGroup getMonthlyServiceEbookGroup() {
		MonthlyServiceEbookGroup group = new MonthlyServiceEbookGroup();
		
		group.setGroups(getGroups());
		return monthlyServiceEbookGroup;
	}

	public void setMonthlyServiceEbookGroup(MonthlyServiceEbookGroup monthlyServiceEbookGroup) {
		this.monthlyServiceEbookGroup = monthlyServiceEbookGroup;
	}

	private List<MonthlyGroup> getGroups(){
		List<MonthlyGroup> group = new ArrayList<MonthlyGroup>();
		return group;
	}
}
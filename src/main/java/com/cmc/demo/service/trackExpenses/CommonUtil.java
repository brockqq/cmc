package com.cmc.demo.service.trackExpenses;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;

import java.io.File;
import java.util.Collection;
import java.util.List;;

public class CommonUtil {
	public static void main(String[] args) {
		JavaProjectBuilder builder = new JavaProjectBuilder();
		builder.addSourceTree(new File("src/main/java/com/cmc/demo/model/entity/response/monthly"));
		System.out.println("====目录下的所有class====");
		Collection<JavaClass> classes = builder.getClasses();
		System.out.println(classes + "\n");
		JavaClass javaClass = builder
				.getClassByName("com.cmc.demo.model.entity.response.monthly.MonthlyServiceResponse");
		// System.out.println(javaClass.getCodeBlock());
		List<JavaField> javaClassFields = javaClass.getFields();
		System.out.println(javaClassFields.get(0).getComment()); // 拿欄位的說明
		System.out.println(javaClassFields.get(0).getModifiers().get(0));//方法是哪種
		System.out.println(javaClassFields.get(0).getType().getName());//是甚麼class
		System.out.println(javaClassFields.get(0).getType().getPackageName() );//class 位置
	}

}

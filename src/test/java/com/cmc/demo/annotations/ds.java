package com.cmc.demo.annotations;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.cmc.demo.model.entity.monthly.MonthlyGroup;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

public class ds {
	public static void main(String [] args) {
		JavaProjectBuilder javaProjectBuilder = new JavaProjectBuilder(); 
		try {
			javaProjectBuilder.addSource(new File("C:\\tmp-works\\workspace\\CMC\\src\\main\\java\\com\\cmc\\demo\\controller\\MonthlyController.java"));
			Collection<JavaClass> classes = javaProjectBuilder.getClasses();
			for (JavaClass javaClass : classes) {
	            // 打印类相关信息
	            System.out.println("类名:" + javaClass.getName());
	            System.out.println("实现了哪些类：" + javaClass.getImplements());
	            System.out.println("继承哪个类：" + javaClass.getSuperJavaClass());
	            System.out.println("注释：" + javaClass.getAnnotations());
	            System.out.println("注释：" + javaClass.getComment());
	            //
	            // 获得方法列表
	            List<JavaMethod> methods = javaClass.getMethods();
	            for (JavaMethod method : methods) {
	                System.out.println("方法名是：" + method.getName());
	                System.out.println("方法的 Tags 有哪些：" + method.getTags().stream().map(it -> it.getName() + "->"+ it.getValue()).collect(Collectors.joining("\n")));
	                System.out.println("方法的参数有哪些：" + method.getParameters());
	                System.out.println("方法的返回值有哪些：" + method.getReturns());
	                System.out.println("方法的Comment有哪些：" + method.getComment());
	                
	            }
	            
	            IntHolder i = new IntHolder(12);

	            System.out.println(i);

	            modify(i);

	            System.out.println(i);
	            Integer i2 = new Integer(1);
	            ds ds =new ds();
	            ds.modifyInt(i2);
	            System.out.println(i2);
	            int[] myInt = { 1 };

	            increment (myInt);

	            System.out.println ("Array contents : " + myInt[0]);
	            MonthlyGroup g = new MonthlyGroup();
	            setMonthlyGroupA(g);
	            System.out.println(g.getContentGroupId());
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void setMonthlyGroupA(MonthlyGroup g) {
		g.setContentGroupId("12322");
	}
    private  int modifyInt(int i) {
    	i = i +1 ;
    			
        System.out.println(i);
        return i;
    }
    public static void increment(int[] array)
    {
       array[0] = array[0] + 1;
    }
    private static void modify(IntHolder i) {

        i.add(1);
        System.out.println(i);
    }
    static class IntHolder {
        private int value;
        public IntHolder(int i) {
            value = i;
        }
        public IntHolder add(int i) {
            value += i;
            return this;
        }
        public String toString() {
            return String.valueOf(value);
        }
    }
}

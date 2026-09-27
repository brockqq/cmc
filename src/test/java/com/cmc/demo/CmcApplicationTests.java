package com.cmc.demo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.cmc.demo.controller.McokDatas;



@SpringBootTest
class CmcApplicationTests {
	final String prefix = "com.cmc.demo.";

	@Test
	void contextLoads() {

		try {
			Class clazz = Class.forName("com.cmc.demo.controller.McokDatas");
			test1231234(clazz.newInstance(), 0);
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException
				| ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void test1231234(final Object instance, int depth) throws IllegalArgumentException, IllegalAccessException {
		final Class<?> clazz = instance.getClass();

		System.out.println(depth + " >" + clazz.getName());
		System.out.println(depth + " >" + clazz.getInterfaces());
		if (!clazz.getName().startsWith(prefix)) {
			return;
		}
		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			final Object member = field.get(instance);
			// System.out.println(member.getClass().getName());
			test1231234(member, depth + 1);
		}
	}
}

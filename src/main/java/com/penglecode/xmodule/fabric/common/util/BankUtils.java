package com.penglecode.xmodule.fabric.common.util;

import java.util.Random;

import org.springframework.util.Assert;

public class BankUtils {

	private static final Random RANDOM = new Random();
	
	/**
	 * 随机生成银行卡号
	 * @param prefix4	- 银行卡号前四位
	 * @return
	 */
	public static String genBankCardNo(String prefix4) {
		Assert.hasText(prefix4, "Parameter 'prefix4' must be required!");
		Assert.isTrue(prefix4.matches("\\d{4}"), "Parameter 'prefix4' must be four-digit!");
		StringBuilder sb = new StringBuilder(prefix4);
		for(int i = 0; i < 12; i++) {
			sb.append(RANDOM.nextInt(10));
		}
		return sb.toString();
	}
	
}

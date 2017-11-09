package com.lzk.gdut.audio.sm;

import java.util.Random;

public class RandomUtil {
	 public static final String ALLCHAR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";  
	 
	 public static String generateString() {  
	        StringBuffer sb = new StringBuffer();  
	        Random random = new Random();  
	        for (int i = 0; i < 16; i++) {  
	            sb.append(ALLCHAR.charAt(random.nextInt(ALLCHAR.length())));  
	        }  
	        return sb.toString();  
	    }  
}

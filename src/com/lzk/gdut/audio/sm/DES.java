package com.lzk.gdut.audio.sm;

import java.security.SecureRandom;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;


public class DES {
    /**
    * ����
    * @param datasource byte[]
    * @param password String
    * @return byte[]
    */
    public static byte[] encrypt(byte[] datasource, String password) { 
    try{
	    SecureRandom random = new SecureRandom();
	    DESKeySpec desKey = new DESKeySpec(password.getBytes());
	    //����һ���ܳ׹�����Ȼ��������DESKeySpecת����
	    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
	    SecretKey securekey = keyFactory.generateSecret(desKey);
	    //Cipher����ʵ����ɼ��ܲ���
	    Cipher cipher = Cipher.getInstance("DES");
	    //���ܳ׳�ʼ��Cipher����
	    cipher.init(Cipher.ENCRYPT_MODE, securekey, random);
	    //���ڣ���ȡ���ݲ�����
	    //��ʽִ�м��ܲ���
	    return cipher.doFinal(datasource);
	    }catch(Throwable e){
	    e.printStackTrace();
    }
    return null;
    }
    /**
    * ����
    * @param src byte[]
    * @param password String
    * @return byte[]
    * @throws Exception
    */
    public static byte[] decrypt(byte[] src, String password) throws Exception {
    	// DES�㷨Ҫ����һ�������ε������Դ
    	SecureRandom random = new SecureRandom();
	    // ����һ��DESKeySpec����
	    DESKeySpec desKey = new DESKeySpec(password.getBytes());
	    // ����һ���ܳ׹���
	    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
	    // ��DESKeySpec����ת����SecretKey����
	    SecretKey securekey = keyFactory.generateSecret(desKey);
	    // Cipher����ʵ����ɽ��ܲ���
	    Cipher cipher = Cipher.getInstance("DES");
	    // ���ܳ׳�ʼ��Cipher����
	    cipher.init(Cipher.DECRYPT_MODE, securekey, random);
	    // ������ʼ���ܲ���
	    return cipher.doFinal(src);
    }

    /** 
     * ��byte����ת��Ϊ��ʾ16����ֵ���ַ����� �磺byte[]{8,18}ת��Ϊ��0813�� ��public static byte[] 
     * hexStr2ByteArr(String strIn) ��Ϊ�����ת������ 

     *  
     * @param arrB 
     *            ��Ҫת����byte���� 

     * @return ת������ַ��� 
     * @throws Exception 
     *             �������������κ��쳣�������쳣ȫ���׳� 
     */  
    private static String byteArr2HexStr(byte[] arrB) throws Exception {  
        int iLen = arrB.length;  
        // ÿ��byte�������ַ����ܱ�ʾ�������ַ����ĳ��������鳤�ȵ�����  

        StringBuffer sb = new StringBuffer(iLen * 2);  
        for (int i = 0; i < iLen; i++) {  
            int intTmp = arrB[i];  
            // �Ѹ���ת��Ϊ����  
            while (intTmp < 0) {  
                intTmp = intTmp + 256;  
            }  
            // С��0F������Ҫ��ǰ�油0  
            if (intTmp < 16) {  
                sb.append("0");  
            }  
            sb.append(Integer.toString(intTmp, 16));  
        }  
        return sb.toString();  
    }  

    /** 
     * ����ʾ16����ֵ���ַ���ת��Ϊbyte���飬 ��public static String byteArr2HexStr(byte[] arrB) 
     * ��Ϊ�����ת������ 

     *  
     * @param strIn 
     *            ��Ҫת�����ַ��� 
     * @return ת�����byte���� 

     * @throws Exception 
     *             �������������κ��쳣�������쳣ȫ���׳� 
     */  
//    private static byte[] hexStr2ByteArr(String strIn) throws Exception {  
//        byte[] arrB = strIn.getBytes();  
//        int iLen = arrB.length;  
//
//        // �����ַ���ʾһ���ֽڣ������ֽ����鳤�����ַ������ȳ���2  
//        byte[] arrOut = new byte[iLen / 2];  
//        for (int i = 0; i < iLen; i = i + 2) {  
//            String strTmp = new String(arrB, i, 2);  
//            arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);  
//        }  
//        return arrOut;  
//    }  

    //���������Կ
    private static String getKey() throws Exception{
        // DES�㷨Ҫ����һ�������ε������Դ
        SecureRandom sr = new SecureRandom();
        // Ϊ����ѡ���DES�㷨����һ��KeyGenerator����
            KeyGenerator kg = KeyGenerator.getInstance("DES");
            kg.init(sr);
            // �����ܳ�
            SecretKey key = kg.generateKey();
            // ��ȡ�ܳ�����
            byte rawKeyData[] = key.getEncoded();
            //return new String(rawKeyData);
            return byteArr2HexStr(rawKeyData);
    }

    /**
     * @param args
     * @throws Exception 
     */
//    public static void main(String[] args) throws Exception {
//
//        //����������
//	        String str = "I AM HAPPY ,ANFYOU ?";
//	        //���룬����Ҫ��8�ı���
//	        String key;
//	
//	        //���������Կ
//	        key=getKey();
//	
//	        String result = DES.encrypt(str.getBytes("UTF8"),key);
//	        System.out.println("���ܺ�"+result);
//	
//	        //ֱ�ӽ��������ݽ���
//	        try {
//		        byte[] decryResult = DES.decrypt(hexStr2ByteArr(result), key);
//		        System.out.println("���ܺ�"+new String(decryResult));
//	        } catch (Exception e1) {
//	        	e1.printStackTrace();
//        }
//    }

}
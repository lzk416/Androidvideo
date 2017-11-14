package com.lzk.gdut.audio.sender;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import xmu.swordbearer.audio.AudioCodec;

import android.R.array;
import android.util.Log;

import com.lzk.gdut.audio.data.AudioData;
import com.lzk.gdut.audio.sm.SM4Utils;

public class AudioEncrypt implements Runnable {
	String LOG = "AudioEncrypt";

	private static AudioEncrypt encrypt;  //先录音后编码
	private boolean isEncrypt = false;
	
	private List<AudioData> dataList = null; 
	
	public static AudioEncrypt getInstance() {
		if (encrypt == null) {
			encrypt = new AudioEncrypt();
		}
		return encrypt;
	}
	
	private AudioEncrypt() {
		dataList = Collections.synchronizedList(new LinkedList<AudioData>());  //用于提供线程安全是
	}
	
	public void addData(byte[] data, int size) { //为dataList添加数据
		AudioData rawData = new AudioData();
		rawData.setSize(size);
		byte[] tempData = new byte[size];
		System.arraycopy(data, 0, tempData, 0, size);
		rawData.setRealData(tempData);
		dataList.add(rawData);
	}
	
	public void startEncrypting() {
		System.out.println(LOG + "开始加密线程");
		if (isEncrypt) {
			return;
		}
		new Thread(this).start();
		Log.e(LOG, "加密开始");
	}
	
	public void stopEncrypting() {
		this.isEncrypt = false;
	}
	
	public void run() {
		SM4Utils sm = new SM4Utils();
		AudioSender sender = new AudioSender();
		sender.startSending();
		
		this.isEncrypt = true;
		byte[] data;
		while (isEncrypt) {
			try {
				while(dataList.size()>0){
					AudioData encodedData = dataList.remove(0);
					data = encodedData.getRealData();
//					String encodedata = new String(data);
//					byte[] encryptdata =  (sm.encryptData_ECB(encodedata)).getBytes();
					
					byte[] encryptdata = sm.encryptData_ECB(data);
					
					sender.addData(encryptdata, encryptdata.length);
				}
			} catch (Exception e) {
				isEncrypt = false;
				e.printStackTrace();
			}
		}
		System.out.println(LOG + "end encrypt");
		sender.stopSending();
	}
}

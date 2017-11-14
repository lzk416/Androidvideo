package com.lzk.gdut.audio.receiver;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import xmu.swordbearer.audio.AudioCodec;

import android.util.Log;

import com.lzk.gdut.audio.data.AudioData;
import com.lzk.gdut.audio.sm.SM4Utils;

public class AudioDecrypt implements Runnable {


	String LOG = "AudioDecrypt";
	private static AudioDecrypt Decrypt;


	private boolean isDecrypt = false;
	private List<AudioData> dataList = null;

	public static AudioDecrypt getInstance() {
		if (Decrypt == null) {
			Decrypt = new AudioDecrypt();
		}
		return Decrypt;
	}

	private AudioDecrypt() {
		this.dataList = Collections
				.synchronizedList(new LinkedList<AudioData>());
	}

	public void addData(byte[] data, int size) {
		AudioData adata = new AudioData();
		adata.setSize(size);
		byte[] tempData = new byte[size];
		System.arraycopy(data, 0, tempData, 0, size);
		adata.setRealData(tempData);
		dataList.add(adata);

	}

	public void startDecrypting() {
		System.out.println(LOG + "开始解密");
		if (isDecrypt) {
			return;
		}
		new Thread(this).start();
	}

	public void run() {
		// start player first
		SM4Utils sm = new SM4Utils();
		AudioDecoder decoder = AudioDecoder.getInstance();
		decoder.startDecoding();
		
		this.isDecrypt = true;

		byte[] data;
		while (isDecrypt) {
			try {
				while(dataList.size()>0){
					AudioData decodedData = dataList.remove(0);
					data = decodedData.getRealData();
					Log.e(LOG, "等待解密数据 " + data.length);
					byte[] decryptdata = sm.decryptData_ECB(data);
					Log.e(LOG, "解密之后的数据" + data.length);
					decoder.addData(decryptdata, decryptdata.length);
				}
			} catch (Exception e) {
				isDecrypt = false;
				e.printStackTrace();
			}
			
		}
		System.out.println(LOG + "stop Decrypt");
		decoder.stopDecoding();
	}

	public void stopDecoding() {
		this.isDecrypt = false;
	}

}

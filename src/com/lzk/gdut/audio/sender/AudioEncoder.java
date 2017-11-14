package com.lzk.gdut.audio.sender;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import xmu.swordbearer.audio.AudioCodec;

import com.lzk.gdut.audio.data.AudioData;

import android.util.Log;

public class AudioEncoder implements Runnable {
	String LOG = "AudioEncoder";

	private static AudioEncoder encoder;  //先录音后编码
	private boolean isEncoding = false; 
	private List<AudioData> dataList = null;   //用来存放编码数据

	
	public static AudioEncoder getInstance() {
		if (encoder == null) {
			encoder = new AudioEncoder();
		}
		return encoder;
	}
	private AudioEncoder() {
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

	/*
	 * start encoding
	 */
	public void startEncoding() {
		System.out.println(LOG + "start encode thread");
		if (isEncoding) {
			Log.e(LOG, "encoder has been started  !!!");
			return;
		}
		new Thread(this).start();
	}

	/*
	 * end encoding
	 */
	public void stopEncoding() {
		this.isEncoding = false;
	}

	public void run() {
		// start sender before encoder
		AudioEncrypt encrypt = AudioEncrypt.getInstance();
		encrypt.startEncrypting();
		
		int encodeSize = 0;
		byte[] encodedData = new byte[256];

		// initialize audio encoder:mode is 30
		AudioCodec.audio_codec_init(30);
		
		isEncoding = true;
		while (isEncoding) {
			if (dataList.size() == 0) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			if (isEncoding) {
				AudioData rawData = dataList.remove(0);
				encodedData = new byte[rawData.getSize()];
				encodeSize = AudioCodec.audio_encode(rawData.getRealData(), 0,
						rawData.getSize(), encodedData, 0);
				if (encodeSize > 0) {
					encrypt.addData(encodedData, encodeSize);
					// clear data
					encodedData = new byte[encodedData.length];
				}
			}
		}
		System.out.println(LOG + "end encoding");
		encrypt.stopEncrypting();
	}

}
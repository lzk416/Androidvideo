package com.lzk.gdut.audio.data;

public class AudioData {  //主要是给addData用的，定义数据的格式
	int size;
	byte[] realData;

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public byte[] getRealData() {
		return realData;
	}

	public void setRealData(byte[] realData) {
		this.realData = realData;
	}

}

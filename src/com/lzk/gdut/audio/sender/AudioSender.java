package com.lzk.gdut.audio.sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import xmu.swordbearer.audio.NetConfig;

import com.lzk.gdut.audio.data.AudioData;

import android.util.Log;

public class AudioSender implements Runnable {
	String LOG = "AudioSender ";

	private boolean isSendering = false;
	private List<AudioData> dataList;

	DatagramSocket socket;  //�����ǽ��պͷ������ݱ�
	DatagramPacket dataPacket; //���ڽ������ݱ�
	private InetAddress ip;
	private int clientPort = 5757;
	public AudioSender() {
		dataList = Collections.synchronizedList(new LinkedList<AudioData>());
		try { 
			try {
				ip = InetAddress.getByName(NetConfig.SERVER_HOST);
				Log.e(LOG, "����˵�ַ�� " + ip.toString());
				socket = new DatagramSocket();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void addData(byte[] data, int size) {
		AudioData encodedData = new AudioData();
		encodedData.setSize(size);
		byte[] tempData = new byte[size];
		System.arraycopy(data, 0, tempData, 0, size);
		encodedData.setRealData(tempData);
		dataList.add(encodedData);
	}

	/*
	 * send data to server
	 */
	private void sendData(byte[] data, int size) {
		try {
			dataPacket = new DatagramPacket(data, size, ip, clientPort);
			dataPacket.setData(data);
			Log.e(LOG, "����һ������ " + data.length);
			socket.send(dataPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * start sending data
	 */
	public void startSending() {
		new Thread(this).start();
	}

	/*
	 * stop sending data
	 */
	public void stopSending() {
		this.isSendering = false;
	}

	// run
	public void run() {
		this.isSendering = true;
		System.out.println(LOG + "start....");
		while (isSendering) {
			if (dataList.size() > 0) {
				AudioData encodedData = dataList.remove(0);
				sendData(encodedData.getRealData(), encodedData.getSize());
			}
		}
		System.out.println(LOG + "stop!!!!");
	}
}
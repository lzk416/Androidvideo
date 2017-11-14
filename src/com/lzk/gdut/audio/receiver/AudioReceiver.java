package com.lzk.gdut.audio.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import xmu.swordbearer.audio.AudioWrapper;
import xmu.swordbearer.audio.NetConfig;


import android.util.Log;

public class AudioReceiver implements Runnable {
	String LOG = "AudioReceiver";
	int port = NetConfig.CLIENT_PORT;// 接收的端口
	DatagramSocket socket;
	DatagramPacket packet;
	boolean isRunning = false;
	private AudioWrapper audioWrapper;

	private byte[] packetBuf = new byte[1024];
	private int packetSize = 1024;

	/*
	 * 开始接收数据
	 */
	public void startRecieving() {
		if (socket == null) {
			try {
				socket = new DatagramSocket(port);
				packet = new DatagramPacket(packetBuf, packetSize);
			} catch (SocketException e) {
			}
		}
		new Thread(this).start();
	}

	/*
	 * 停止接收数据
	 */
	public void stopRecieving() {
		isRunning = false;
	}

	/*
	 * 释放资源
	 */
	private void release() {
		if (packet != null) {
			packet = null;
		}
		if (socket != null) {
			socket.close();
			socket = null;
		}
	}

	public void run() {
		// 在接收前，要先启动解码器
		AudioDecrypt decrypt = AudioDecrypt.getInstance();
		decrypt.startDecrypting();
		isRunning = true;
		try {
			while (isRunning) {
				socket.receive(packet);
				InetAddress address = packet.getAddress();
				int length = packet.getLength();
				byte[] data = packet.getData();
				int port2 = packet.getPort();
				// 每接收一个UDP包，就交给解码器，等待解码
				decrypt.addData(data, length);
			}

		} catch (IOException e) {
			Log.e(LOG, "RECIEVE ERROR!");
		}
		// 接收完成，停止解码器，释放资源
		decrypt.stopDecoding();
		release();
		Log.e(LOG, "stop recieving");
	}
}

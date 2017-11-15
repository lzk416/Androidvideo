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
	int port = NetConfig.CLIENT_PORT;// ���յĶ˿�
	DatagramSocket socket;
	DatagramPacket packet;
	boolean isRunning = false;
	private AudioWrapper audioWrapper;

	private byte[] packetBuf = new byte[1024];
	private int packetSize = 1024;

	/*
	 * ��ʼ��������
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
	 * ֹͣ��������
	 */
	public void stopRecieving() {
		isRunning = false;
	}

	/*
	 * �ͷ���Դ
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
		// �ڽ���ǰ��Ҫ������������
		AudioDecrypt decrypt = AudioDecrypt.getInstance();
		decrypt.startDecrypting();
		isRunning = true;
		try {
			while (isRunning) {
				socket.receive(packet);
				int length = packet.getLength();
				Log.e(LOG,"�յ��İ��ĳ��ȣ� "+length);
				byte[] data = packet.getData();
				// ÿ����һ��UDP�����ͽ������������ȴ�����
				decrypt.addData(data, length);
			}

		} catch (IOException e) {
			Log.e(LOG, "RECIEVE ERROR!");
		}
		// ������ɣ�ֹͣ���������ͷ���Դ
		decrypt.stopDecoding();
		release();
		Log.e(LOG, "stop recieving");
	}
}

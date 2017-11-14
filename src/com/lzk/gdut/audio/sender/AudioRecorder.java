package com.lzk.gdut.audio.sender;

import xmu.swordbearer.audio.AudioConfig;

import android.media.AudioRecord;
import android.util.Log;

public class AudioRecorder implements Runnable {

	String LOG = "Recorder ";

	private boolean isRecording = false;
	private AudioRecord audioRecord; //实例化一个变量

	private static final int BUFFER_FRAME_SIZE = 480;
	private int audioBufSize = 0;

	private byte[] samples;// data
	private int bufferRead = 0;
	private int bufferSize = 0;
	

	/*
	 * start recording
	 */
	public void startRecording() {
		bufferSize = BUFFER_FRAME_SIZE;

		audioBufSize = AudioRecord.getMinBufferSize(AudioConfig.SAMPLERATE,
				AudioConfig.RECORDER_CHANNEL_CONFIG, AudioConfig.AUDIO_FORMAT);
		if (audioBufSize == AudioRecord.ERROR_BAD_VALUE) {
			Log.e(LOG, "audioBufSize error");
			return;
		}
		samples = new byte[audioBufSize];
		// 初始化recorder
		if (null == audioRecord) {
			audioRecord = new AudioRecord
					(AudioConfig.AUDIO_RESOURCE, //音频源，从哪里采集音频
					AudioConfig.SAMPLERATE,		//音频的采样频率
					AudioConfig.RECORDER_CHANNEL_CONFIG, //声道设置
					AudioConfig.AUDIO_FORMAT,//编码制式和采样大小
					audioBufSize);//采集数据需要的缓冲区的大小
		}
		new Thread(this).start();
	}

	/*
	 * stop
	 */
	public void stopRecording() {
		this.isRecording = false;
	}

	public boolean isRecording() {
		return isRecording;
	}

	public void run() {
		// start encoder before recording
		AudioEncoder encoder = AudioEncoder.getInstance();
		encoder.startEncoding();
		System.out.println(LOG + "audioRecord startRecording()");
		audioRecord.startRecording();
		System.out.println(LOG + "start recording");
		this.isRecording = true;
		while (isRecording) {
			bufferRead = audioRecord.read(samples, 0, bufferSize);
			if (bufferRead > 0) {
				// add data to encoder
				encoder.addData(samples, bufferRead);  //data and data.length()
 			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(LOG + "end recording");
		audioRecord.stop();
		encoder.stopEncoding();
	}
	


}

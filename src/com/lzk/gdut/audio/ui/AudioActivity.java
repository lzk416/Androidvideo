package com.lzk.gdut.audio.ui;

import xmu.swordbearer.audio.AudioWrapper;
import xmu.swordbearer.audio.NetConfig;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import com.ty.winchat.R;
import com.ty.winchat.ui.Main;
import com.ty.winchat.ui.VoiceAndVideo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AudioActivity extends Activity {
	String LOG = "MainActivity";
	private AudioWrapper audioWrapper;
	VoiceAndVideo voice = new VoiceAndVideo();
	// View
	private Button btnStartRecord;
	private Button btnStopRecord;
	private Button btnStartListen;
	private Button btnStopListen;
	private Button btnExit;
	

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voice);
		audioWrapper = AudioWrapper.getInstance();
		initView();
		Toast.makeText(this, getLocalIpAddress(), Toast.LENGTH_SHORT).show();
	}

	private void initView() {
		btnStartRecord = (Button) findViewById(R.id.startRecord);
		btnStartListen = (Button) findViewById(R.id.startListen);
		btnStopRecord = (Button) findViewById(R.id.stopRecord);
		btnStopListen = (Button) findViewById(R.id.stopListen);
		btnExit = (Button) findViewById(R.id.btnExit); //离开按钮
		String ipString = getIntent().getStringExtra("IP");
		NetConfig.setServerHost(ipString);
		//对按钮设置使能。
		btnStopRecord.setEnabled(false);
		btnStopListen.setEnabled(false);
		
		btnStartRecord.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				btnStartRecord.setEnabled(false);
				btnStopRecord.setEnabled(true);
				audioWrapper.startRecord();
			}
		});

		btnStopRecord.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				btnStartRecord.setEnabled(true);
				btnStopRecord.setEnabled(false);
				audioWrapper.stopRecord();
				audioWrapper.stopListen();
			}
		});
		btnStartListen.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				btnStartListen.setEnabled(false);
				btnStopListen.setEnabled(true);
				audioWrapper.startListen();
			}
		});
		btnStopListen.setOnClickListener(new View.OnClickListener() {  //Listener是开始接受的意思。
			public void onClick(View arg0) {
				btnStartListen.setEnabled(true);
				btnStopListen.setEnabled(false);
				audioWrapper.stopListen();
			}
		});
		btnExit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				audioWrapper.stopListen();
				audioWrapper.stopRecord();
				voice.setFlag(false);
				Intent intent=new Intent(AudioActivity.this,Main.class );
				startActivity(intent);
			}
		});
	}
	
	public static String getLocalIpAddress(){
		try{
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); 
			while(en.hasMoreElements()){
				NetworkInterface nif = en.nextElement();
				Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();
				while(enumIpAddr.hasMoreElements()){
					InetAddress mInetAddress = enumIpAddr.nextElement();
					if(!mInetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress())){
						return mInetAddress.getHostAddress();
					}
				}
			}
		}catch(SocketException e){
			e.printStackTrace();
		}
		return null;
	}
}
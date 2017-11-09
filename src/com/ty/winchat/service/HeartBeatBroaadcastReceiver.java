package com.ty.winchat.service;

import com.ty.winchat.ui.VoiceAndVideo.MessageUpdateBroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 心跳包检测，只检测通信双方
 * @author wj
 * @creation 2013-5-9
 */
public class HeartBeatBroaadcastReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent intent2=new Intent();
		intent2.setAction(MessageUpdateBroadcastReceiver.ACTION_HEARTBEAT);
		context.sendBroadcast(intent2);
	}
}

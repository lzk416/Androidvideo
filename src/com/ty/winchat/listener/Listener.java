package com.ty.winchat.listener;

import java.io.IOException;

public abstract class Listener extends Thread{
	
	public static final int NONE=0;//没有动作
	
	public static final int ADD_USER=1;//增加用户
	public static final int LOGIN_SUCC=2;//增加用户成功
	public static final int REMOVE_USER=3;//删除用户
	
	public static final int RECEIVE_MSG=4;//接收消息
	public static final int RECEIVE_FILE=5;//接收文件
	
	public static final int HEART_BEAT=6;//发送心跳包
	public static final int HEART_BEAT_REPLY=7;//心跳包回复
	
	public static final int ASK_SEND_FILE=8;//请求发送文件
	public static final int REPLAY_SEND_FILE=9;//回复请求发送文件
	
	public static final int REQUIRE_ICON=10;//请求发送头像
	
	public static final int ASK_VIDEO=11;//请求视屏聊天
	public static final int REPLAY_VIDEO_ALLOW=12;//请求视屏聊天
	public static final int REPLAY_VIDEO_NOT_ALLOW=13;//请求视屏聊天
	
	public static final int  KEY_EXCHANGE=15;//请求公钥交换
	public static final int ACK_RECEIVE=16;//私钥收到
	
	public static final int ASK_VOICE=17;//请求语音聊天
	public static final int REPLAY_VOICE_ALLOW=18;//请求语音聊天
	public static final int REPLAY_VOICE_NOT_ALLOW=19;//请求语音聊天
	
	public static final int BEGIN_RECEIVE_VOICE=20;//开始接收语音数据
	public static final int END_RECEIVE_VOICE=21;//结束接收语音数据
	
	/**打开监听器*/
	abstract void open() throws IOException;
	/**关闭监听器*/
	abstract void close() throws IOException;
}

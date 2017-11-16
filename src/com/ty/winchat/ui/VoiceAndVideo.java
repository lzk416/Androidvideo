package com.ty.winchat.ui;


import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import org.json.JSONObject;

import com.lzk.gdut.audio.sm.RandomUtil;
import com.lzk.gdut.audio.sm.SM2Utils;
import com.lzk.gdut.audio.sm.SM4Utils;
import com.lzk.gdut.audio.sm.Util;
import com.nercms.VideoChatActivity;
import com.ty.winchat.R;
import com.ty.winchat.WinChatApplication;
import com.ty.winchat.listener.Listener;
import com.ty.winchat.listener.TCPFileListener;
import com.ty.winchat.model.FileMessage;
import com.ty.winchat.model.UDPMessage;
import com.ty.winchat.model.User;
import com.ty.winchat.service.ChatService;
import com.ty.winchat.service.HeartBeatBroaadcastReceiver;
import com.ty.winchat.service.ChatService.MyBinder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class VoiceAndVideo extends Base implements OnClickListener{
	
	private static final String TAG =  "KEYCHANGE";
	//请求语音则为true，请求视频即为false
	private boolean flag = false;
	private Button voice,video;
	
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
	private ListView listView;
	private MyBinder binder;
	private User chatter;//对方聊天人
	private List<UDPMessage> myMessages=new ArrayList<UDPMessage>();//保存聊天信息
	private MyServiceConnection connection;
	private boolean started;//用来标识控件是否渲染完毕
	private String chatterDeviceCode;//记录当前用户设备id
	private String chatterIP;//记录当前用户ip
	private TextView topTitle;
	private TCPFileListener fileListener;
	private MessageUpdateBroadcastReceiver receiver=new MessageUpdateBroadcastReceiver();
	private AlarmManager alarmManager;//用来发送心跳包
	private PendingIntent pendingIntent;
	//PopupWindow即可为弹出的提示框
	private PopupWindow popupWindow;
	private final int SHOW_DIALOG=0XF1001;
	
	private Handler handler=new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case SHOW_DIALOG:
				if(popupWindow!=null) popupWindow.showAtLocation(listView, Gravity.CENTER, 0, 0);
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.voiceandvideo);
	  chatterIP=getIntent().getStringExtra("IP");
	  chatterDeviceCode=getIntent().getStringExtra("DeviceCode");
	  findViews();
	  init();
	  ((WinChatApplication)getApplication()).createDir();
	}
	
	
	private void init() {
		 //绑定到service
		  Intent intent=new Intent(VoiceAndVideo.this,ChatService.class);
		  //在这里绑定之后，就都可以获得那个bind对象，即可获得里面的方法
		  bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
		  //注册更新广播
		  IntentFilter filter=new IntentFilter();
		  filter.addAction(MessageUpdateBroadcastReceiver.ACTION_NOTIFY_DATA);
		  filter.addAction(MessageUpdateBroadcastReceiver.ACTION_HEARTBEAT);
		  registerReceiver(receiver, filter);
		  //开启心跳包
		  alarmManager=(AlarmManager) getSystemService(ALARM_SERVICE);
		  pendingIntent= PendingIntent.getBroadcast(this, 0, new Intent(this,HeartBeatBroaadcastReceiver.class), 0);
		  alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), User.INTERVAL, pendingIntent);
	}


	private void findViews() {
		
		listView=(ListView) findViewById(R.id.videoandvocicelistview);
		topTitle=(TextView) findViewById(R.id.toptextView);
		topTitle.setText(getIntent().getStringExtra("name"));
		listView.setDivider(null);
		voice=(Button) findViewById(R.id.voice);
		video=(Button) findViewById(R.id.video);
		voice.setOnClickListener(this);
		video.setOnClickListener(this);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.video:
			sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.ASK_VOICE));
			closeMorePopupWindow();
			showToast("已发送请求，对方同意后自动进行语音聊天");
			break;
		case R.id.voice:
			sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.ASK_VIDEO));
			closeMorePopupWindow();
			showToast("已发送请求，对方同意后自动进行视屏聊天");
			break;
		}
	}
	private PopupWindow morePopupWindow ;
	private void closeMorePopupWindow(){
		if(morePopupWindow!=null)
			morePopupWindow.dismiss();
	}
	
	@Override
	protected void onDestroy() {
	  super.onDestroy();
	  unbindService(connection);
	  unregisterReceiver(receiver);
	  alarmManager.cancel(pendingIntent);
	   if(myMessages!=null) myMessages.clear();
	}
	/**
	 * 发送消息
	 * @param txt
	 */
	private void sendMsg(UDPMessage msg){
		if(binder!=null){
			if(WinChatApplication.mainInstance.getLocalIp().equals(chatterIP)){
				if(chatter==null){
					chatter=new User();
					chatter.setDeviceCode(WinChatApplication.mainInstance.getDeviceCode());
					chatter.setUserName(WinChatApplication.mainInstance.getMyName());
					chatter.setIp(chatterIP);
				}
			}else {
				chatter=binder.getUsers().get(chatterIP);
			}
			//对方下线 ||（在线&&心跳包检测超时）—>网络断开
			if(chatter==null||(chatter!=null&&!chatter.checkOnline())){
				Toast.makeText(this, "对方已不在线", Toast.LENGTH_SHORT).show();
				binder.getUsers().remove(chatterIP);
				sendBroadcast(new Intent(Main.ACTION_ADD_USER));
			}
			try {
				if(chatter!=null&&chatterDeviceCode.equals(chatter.getDeviceCode()))
					binder.sendMsg(msg, InetAddress.getByName(chatterIP));
				if(Listener.RECEIVE_MSG==Integer.valueOf(msg.getType()))//如果是文本消息
					myMessages.add(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			 unbindService(connection);
			 Intent intent=new Intent(VoiceAndVideo.this,ChatService.class);
			 bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
			 Toast.makeText(this, "未发送出去,请重新发送", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	public  class MyServiceConnection implements ServiceConnection{
		@Override
    public void onServiceConnected(ComponentName name, IBinder service) {
			//这样就获得service的实例了
			binder=(MyBinder) service;
			if(WinChatApplication.mainInstance.getLocalIp().equals(chatterIP)){
				chatter=new User();
				chatter.setDeviceCode(WinChatApplication.mainInstance.getDeviceCode());
				chatter.setUserName(WinChatApplication.mainInstance.getMyName());
				chatter.setIp(chatterIP);
			}else {
				chatter=binder.getUsers().get(chatterIP);
			}
			chatterDeviceCode=chatter.getDeviceCode();
			Queue<UDPMessage> queue=binder.getMessages().get(chatter.getIp());
			if(queue!=null){//从后台遍历读取数据
				try {
					ergodicMessage(queue);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			started=true;
    }

		@Override
    public void onServiceDisconnected(ComponentName name) {
    }
  	
  }
	
	/**
	 * 从后台遍历消息
	 * @param queue
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	private void ergodicMessage(Queue<UDPMessage> queue) throws IllegalArgumentException, IOException{
		Iterator<UDPMessage> iterator=queue.iterator();
		UDPMessage message;
		while(iterator.hasNext()){
			message=iterator.next();
			Log.e(TAG, "类型："+message.getType());
			switch (message.getType()) {
			
				
				case Listener.RECEIVE_MSG:
					myMessages.add(message);
					break;
					
				case Listener.ASK_VOICE:
					showDialog("对方请求语音,同意吗？", new OnClickListener() {
						@Override
						public void onClick(View v) {
							flag=true;
							showToast("正在发送向对方发送SM2算法公钥");
							SM2Utils.generateKeyPair();
							String pubk = SM2Utils.getPubk();
							sendMsg(WinChatApplication.mainInstance.getMyUdpMessage(pubk, Listener.REPLAY_VOICE_ALLOW));
							if(popupWindow!=null) popupWindow.dismiss();
						}
					}, new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.REPLAY_VIDEO_NOT_ALLOW));
							if(popupWindow!=null) popupWindow.dismiss();
						}
					}, true);
					break;
					
				case Listener.ASK_VIDEO:
					showDialog("对方请求视屏,同意吗？", new OnClickListener() {
						@Override
						public void onClick(View v) {
							flag=false;
							showToast("正在发送向对方发送SM2算法公钥");
							SM2Utils.generateKeyPair();
							String pubk = SM2Utils.getPubk();
							sendMsg(WinChatApplication.mainInstance.getMyUdpMessage(pubk, Listener.REPLAY_VIDEO_ALLOW));
							if(popupWindow!=null) popupWindow.dismiss();
						}
					}, new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.REPLAY_VIDEO_NOT_ALLOW));
							if(popupWindow!=null) popupWindow.dismiss();
						}
					}, true);
					break;
					
				
				case Listener.REPLAY_VOICE_ALLOW:
					 flag=true;
					 String key = RandomUtil.generateString();
					 SM4Utils.setSecretKey(key);
					 showToast("收到对方公钥,正在发送SM4秘钥: "+key);
					 byte[] plainText =key.getBytes();
					 String secretkey =  SM2Utils.encrypt(Util.hexToByte(message.getMsg()), plainText);
					 sendMsg(WinChatApplication.mainInstance.getMyUdpMessage(secretkey, Listener.KEY_EXCHANGE));
					 if(popupWindow!=null) popupWindow.dismiss();
					break;
					
				case Listener.REPLAY_VIDEO_ALLOW:
					 flag=false;
					 String key1 = RandomUtil.generateString();
					 SM4Utils.setSecretKey(key1);
					 showToast("收到对方公钥,正在发送SM4秘钥: "+key1);
					 byte[] plainText1 =key1.getBytes();
					 String secretkey1 =  SM2Utils.encrypt(Util.hexToByte(message.getMsg()), plainText1);
					sendMsg(WinChatApplication.mainInstance.getMyUdpMessage(secretkey1, Listener.KEY_EXCHANGE));
					if(popupWindow!=null) popupWindow.dismiss();
					break;
					
				case Listener.ACK_RECEIVE:
					showToast("秘钥交换完成");
					if(flag) {
						Intent intent3=new Intent(VoiceAndVideo.this,VoiceChat.class );
						intent3.putExtra("name", topTitle.getText().toString());
						intent3.putExtra("IP", chatterIP);
						intent3.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent3);
					}else {
						Intent intent4=new Intent(VoiceAndVideo.this,VideoChatActivity.class );
						intent4.putExtra("name", topTitle.getText().toString());
						intent4.putExtra("remote_ip", chatterIP);
						intent4.putExtra("remote_port", 19888);
						intent4.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent4);
					}
					
					break;
					
				case Listener.KEY_EXCHANGE:
					String str = new String(SM2Utils.decrypt(Util.hexToByte(SM2Utils.getPrik()),Util.hexToByte(message.getMsg())));
					showToast("收到SM4秘钥： "+str);
					SM4Utils.setSecretKey(str);
					sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.ACK_RECEIVE));
					if(flag) {
						Intent intent1=new Intent(VoiceAndVideo.this,VoiceChat.class );
						intent1.putExtra("name", topTitle.getText().toString());
						intent1.putExtra("IP", chatterIP);
						intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent1);
					}else {
						Intent intent2=new Intent(VoiceAndVideo.this,VideoChatActivity.class );
						intent2.putExtra("name", topTitle.getText().toString());
						intent2.putExtra("remote_ip", chatterIP);
						intent2.putExtra("remote_port", 19888);
						intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent2);
					}
					
					break;
					
			
					
			
				case Listener.REPLAY_VOICE_NOT_ALLOW:	
				case Listener.REPLAY_VIDEO_NOT_ALLOW:
					showToast("对方拒绝视屏");
					break;
				case Listener.REPLAY_SEND_FILE:
					try {
						  String msg=message.getMsg();
				          FileMessage fileMessage=new FileMessage(new JSONObject(msg));
				          if(FileMessage.ALLOW_SEND_FILE.equals(fileMessage.getAllow())){//同意发送文件
				              	fileListener.sendFile(chatterIP, new File(fileMessage.getFilePath()),WinChatApplication.mainInstance.getFilePath(),null);//发送文件
				          }else{//不同意发送文件
										
						}
		          } catch (Exception e) {
			          e.printStackTrace();
		          }
					break;
			}
		}
		queue.clear();
	}
	
	
	//显示提醒框的高宽
	private final int width=WinChatApplication.width*3/4;
	private final int height=WinChatApplication.height*2/7;
	/**
	 * 显示提醒框
	 * @param txt
	 * @param ok
	 */
	private void showDialog(String txt,OnClickListener ok,OnClickListener cancl,boolean buttonShow){
		if(popupWindow!=null)
			popupWindow.dismiss();
		popupWindow=new PopupWindow(getApplicationContext());
		popupWindow.setWidth(width);
		popupWindow.setHeight(height);
		popupWindow.setFocusable(false);
		popupWindow.setOutsideTouchable(false);
		popupWindow.setBackgroundDrawable(new BitmapDrawable());// 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
		View view= getLayoutInflater().inflate(R.layout.confirm_dialog, null);
		TextView textView=(TextView) view.findViewById(R.id.confirm_dialog_txt);
		Button confirm=(Button) view.findViewById(R.id.confirm_dialog_confirm);
		Button cancle=(Button) view.findViewById(R.id.confirm_dialog_cancle);
		if(!buttonShow){
			confirm.setVisibility(View.INVISIBLE);
			cancle.setVisibility(View.INVISIBLE);
		}else {
			confirm.setOnClickListener(ok);
			cancle.setOnClickListener(cancl);
		}
		popupWindow.setContentView(view);
		textView.setText(txt);
		if(started)//Activity已经渲染完毕
			popupWindow.showAtLocation(listView, Gravity.CENTER, 0, 0);
		else {//Activity还未渲染完毕
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while(!started){
							Thread.sleep(500);
						}
						handler.sendEmptyMessage(SHOW_DIALOG);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	  public class MessageUpdateBroadcastReceiver extends BroadcastReceiver{
		  public static final String ACTION_HEARTBEAT="com.ty.winchat.heartbeat";
		  public static final String ACTION_NOTIFY_DATA="com.ty.winchat.notifydata";
		  
			@Override
			public void onReceive(Context context, Intent intent) {
				
				if(intent!=null&&ACTION_HEARTBEAT.equals(intent.getAction())){//心跳包检测
					if(binder!=null)
						try {
							binder.sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.HEART_BEAT), InetAddress.getByName(chatterIP));//发送心跳包
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					return;
				}else if(ACTION_NOTIFY_DATA.equals(intent.getAction())){//刷新消息
						if(binder!=null){
							Queue<UDPMessage> queue=binder.getMessages().get(chatterIP);
							if(queue!=null)
								try {
									ergodicMessage(queue);
								} catch (IllegalArgumentException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
						}else {
							 unbindService(connection);
							 Intent intent1=new Intent(VoiceAndVideo.this,ChatService.class);
							 bindService(intent1, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
						}
				}
	    }
			
		}
	
}

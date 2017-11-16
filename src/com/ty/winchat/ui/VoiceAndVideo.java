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
	//����������Ϊtrue��������Ƶ��Ϊfalse
	private boolean flag = false;
	private Button voice,video;
	
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
	private ListView listView;
	private MyBinder binder;
	private User chatter;//�Է�������
	private List<UDPMessage> myMessages=new ArrayList<UDPMessage>();//����������Ϣ
	private MyServiceConnection connection;
	private boolean started;//������ʶ�ؼ��Ƿ���Ⱦ���
	private String chatterDeviceCode;//��¼��ǰ�û��豸id
	private String chatterIP;//��¼��ǰ�û�ip
	private TextView topTitle;
	private TCPFileListener fileListener;
	private MessageUpdateBroadcastReceiver receiver=new MessageUpdateBroadcastReceiver();
	private AlarmManager alarmManager;//��������������
	private PendingIntent pendingIntent;
	//PopupWindow����Ϊ��������ʾ��
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
		 //�󶨵�service
		  Intent intent=new Intent(VoiceAndVideo.this,ChatService.class);
		  //�������֮�󣬾Ͷ����Ի���Ǹ�bind���󣬼��ɻ������ķ���
		  bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
		  //ע����¹㲥
		  IntentFilter filter=new IntentFilter();
		  filter.addAction(MessageUpdateBroadcastReceiver.ACTION_NOTIFY_DATA);
		  filter.addAction(MessageUpdateBroadcastReceiver.ACTION_HEARTBEAT);
		  registerReceiver(receiver, filter);
		  //����������
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
			showToast("�ѷ������󣬶Է�ͬ����Զ�������������");
			break;
		case R.id.voice:
			sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.ASK_VIDEO));
			closeMorePopupWindow();
			showToast("�ѷ������󣬶Է�ͬ����Զ�������������");
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
	 * ������Ϣ
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
			//�Է����� ||������&&��������ⳬʱ����>����Ͽ�
			if(chatter==null||(chatter!=null&&!chatter.checkOnline())){
				Toast.makeText(this, "�Է��Ѳ�����", Toast.LENGTH_SHORT).show();
				binder.getUsers().remove(chatterIP);
				sendBroadcast(new Intent(Main.ACTION_ADD_USER));
			}
			try {
				if(chatter!=null&&chatterDeviceCode.equals(chatter.getDeviceCode()))
					binder.sendMsg(msg, InetAddress.getByName(chatterIP));
				if(Listener.RECEIVE_MSG==Integer.valueOf(msg.getType()))//������ı���Ϣ
					myMessages.add(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			 unbindService(connection);
			 Intent intent=new Intent(VoiceAndVideo.this,ChatService.class);
			 bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
			 Toast.makeText(this, "δ���ͳ�ȥ,�����·���", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	public  class MyServiceConnection implements ServiceConnection{
		@Override
    public void onServiceConnected(ComponentName name, IBinder service) {
			//�����ͻ��service��ʵ����
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
			if(queue!=null){//�Ӻ�̨������ȡ����
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
	 * �Ӻ�̨������Ϣ
	 * @param queue
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	private void ergodicMessage(Queue<UDPMessage> queue) throws IllegalArgumentException, IOException{
		Iterator<UDPMessage> iterator=queue.iterator();
		UDPMessage message;
		while(iterator.hasNext()){
			message=iterator.next();
			Log.e(TAG, "���ͣ�"+message.getType());
			switch (message.getType()) {
			
				
				case Listener.RECEIVE_MSG:
					myMessages.add(message);
					break;
					
				case Listener.ASK_VOICE:
					showDialog("�Է���������,ͬ����", new OnClickListener() {
						@Override
						public void onClick(View v) {
							flag=true;
							showToast("���ڷ�����Է�����SM2�㷨��Կ");
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
					showDialog("�Է���������,ͬ����", new OnClickListener() {
						@Override
						public void onClick(View v) {
							flag=false;
							showToast("���ڷ�����Է�����SM2�㷨��Կ");
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
					 showToast("�յ��Է���Կ,���ڷ���SM4��Կ: "+key);
					 byte[] plainText =key.getBytes();
					 String secretkey =  SM2Utils.encrypt(Util.hexToByte(message.getMsg()), plainText);
					 sendMsg(WinChatApplication.mainInstance.getMyUdpMessage(secretkey, Listener.KEY_EXCHANGE));
					 if(popupWindow!=null) popupWindow.dismiss();
					break;
					
				case Listener.REPLAY_VIDEO_ALLOW:
					 flag=false;
					 String key1 = RandomUtil.generateString();
					 SM4Utils.setSecretKey(key1);
					 showToast("�յ��Է���Կ,���ڷ���SM4��Կ: "+key1);
					 byte[] plainText1 =key1.getBytes();
					 String secretkey1 =  SM2Utils.encrypt(Util.hexToByte(message.getMsg()), plainText1);
					sendMsg(WinChatApplication.mainInstance.getMyUdpMessage(secretkey1, Listener.KEY_EXCHANGE));
					if(popupWindow!=null) popupWindow.dismiss();
					break;
					
				case Listener.ACK_RECEIVE:
					showToast("��Կ�������");
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
					showToast("�յ�SM4��Կ�� "+str);
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
					showToast("�Է��ܾ�����");
					break;
				case Listener.REPLAY_SEND_FILE:
					try {
						  String msg=message.getMsg();
				          FileMessage fileMessage=new FileMessage(new JSONObject(msg));
				          if(FileMessage.ALLOW_SEND_FILE.equals(fileMessage.getAllow())){//ͬ�ⷢ���ļ�
				              	fileListener.sendFile(chatterIP, new File(fileMessage.getFilePath()),WinChatApplication.mainInstance.getFilePath(),null);//�����ļ�
				          }else{//��ͬ�ⷢ���ļ�
										
						}
		          } catch (Exception e) {
			          e.printStackTrace();
		          }
					break;
			}
		}
		queue.clear();
	}
	
	
	//��ʾ���ѿ�ĸ߿�
	private final int width=WinChatApplication.width*3/4;
	private final int height=WinChatApplication.height*2/7;
	/**
	 * ��ʾ���ѿ�
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
		popupWindow.setBackgroundDrawable(new BitmapDrawable());// �����Ϊ�˵��������Back��Ҳ��ʹ����ʧ�����Ҳ�����Ӱ����ı���
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
		if(started)//Activity�Ѿ���Ⱦ���
			popupWindow.showAtLocation(listView, Gravity.CENTER, 0, 0);
		else {//Activity��δ��Ⱦ���
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
				
				if(intent!=null&&ACTION_HEARTBEAT.equals(intent.getAction())){//���������
					if(binder!=null)
						try {
							binder.sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.HEART_BEAT), InetAddress.getByName(chatterIP));//����������
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					return;
				}else if(ACTION_NOTIFY_DATA.equals(intent.getAction())){//ˢ����Ϣ
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

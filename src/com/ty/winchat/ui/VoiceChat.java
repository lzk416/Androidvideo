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
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import xmu.swordbearer.audio.AudioWrapper;
import xmu.swordbearer.audio.NetConfig;

public class VoiceChat  extends Base implements OnClickListener{
	private static final String TAG =  "VoiceChat";
	private AudioWrapper audioWrapper = AudioWrapper.getInstance(); ;
	//����������Ϊtrue��������Ƶ��Ϊfalse
	private Button end,begin;
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
	private MessageUpdateVoiceBroadcastReceiver receiver=new MessageUpdateVoiceBroadcastReceiver();
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
	  setContentView(R.layout.begin_and_end_voice);
	  chatterIP=getIntent().getStringExtra("IP");
	  chatterDeviceCode=getIntent().getStringExtra("DeviceCode");
	  NetConfig.setServerHost(chatterIP);
	  findViews();
	  init();
	  ((WinChatApplication)getApplication()).createDir();
	}
	
	
	private void init() {
		 //�󶨵�service
		  Intent intent=new Intent(VoiceChat.this,ChatService.class);
		  //�������֮�󣬾Ͷ����Ի���Ǹ�bind���󣬼��ɻ������ķ���
		  bindService(intent, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
		  //ע����¹㲥
		  IntentFilter filter=new IntentFilter();
		  filter.addAction(MessageUpdateVoiceBroadcastReceiver.ACTION_NOTIFY_DATA);
		  filter.addAction(MessageUpdateVoiceBroadcastReceiver.ACTION_HEARTBEAT);
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
		end=(Button) findViewById(R.id.end);
		begin=(Button) findViewById(R.id.begin);
		end.setOnClickListener(this);
		begin.setOnClickListener(this);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.begin:
			begin.setBackgroundResource(R.drawable.voice_ing);
			audioWrapper.startRecord();
			sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.BEGIN_RECEIVE_VOICE));
			closeMorePopupWindow();
			break;
		case R.id.end:
			begin.setBackgroundResource(R.drawable.begin_voice);
			audioWrapper.stopRecord();
			sendMsg(WinChatApplication.mainInstance.getMyUdpMessage("", Listener.END_RECEIVE_VOICE));
			closeMorePopupWindow();
			finish();
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
			 Intent intent=new Intent(VoiceChat.this,ChatService.class);
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
					
				
				case Listener.BEGIN_RECEIVE_VOICE:
					audioWrapper.startListen();
					showDialog("�Է����ڽ����У���ע���������");
					break;
					
				case Listener.END_RECEIVE_VOICE:
					audioWrapper.stopListen();
					if(popupWindow!=null) popupWindow.dismiss();
					finish();
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
	private void showDialog(String txt){
		if(popupWindow!=null)
			popupWindow.dismiss();
		popupWindow=new PopupWindow(getApplicationContext());
		popupWindow.setWidth(width);
		popupWindow.setHeight(height);
		popupWindow.setFocusable(false);
		popupWindow.setOutsideTouchable(false);
		View view= getLayoutInflater().inflate(R.layout.voice_dialog, null);
		TextView textView=(TextView) view.findViewById(R.id.confirm_dialog_txt);
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
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			audioWrapper.stopListen();
			audioWrapper.stopRecord();
			finish();
		}
		return true;
	}
	
	  public class MessageUpdateVoiceBroadcastReceiver extends BroadcastReceiver{
		  public static final String ACTION_HEARTBEAT="voice.heartbeat";
		  public static final String ACTION_NOTIFY_DATA="voice.notifydata";
		  
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
							 Intent intent1=new Intent(VoiceChat.this,ChatService.class);
							 bindService(intent1, connection=new MyServiceConnection(), Context.BIND_AUTO_CREATE);
						}
				}
	    }
			
		}
}

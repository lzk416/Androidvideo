package xmu.swordbearer.audio;

import android.util.Log;

/*
 * UDP configure
 */
public class NetConfig {
	public static String SERVER_HOST = "192.168.200.81";// server ip
	public static final int SERVER_PORT = 5656;// server port
	public static final int CLIENT_PORT = 5757;// client port

	public static void setServerHost(String ip) {  //����ͨ���û��˽�������
		Log.e("NetConfig", "�������õ�IP��" + ip);
		SERVER_HOST = ip;
	}
}

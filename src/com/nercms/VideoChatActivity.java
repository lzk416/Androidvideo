package com.nercms;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.lzk.gdut.audio.sm.SM4Utils;
import com.nercms.receive.Receive;
import com.nercms.receive.VideoPlayView;
import com.nercms.send.Send;
import com.ty.winchat.R;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * ��Ƶ����
 * Created by zsg on 2016/6/3.
 */
public class VideoChatActivity extends Activity {
	private TextView topTitle;
    private VideoPlayView view = null;
    private SurfaceView surfaceView;
    private Camera mCamera = null; //��������ͷ������
    private SurfaceHolder holder = null; //��������������ʾ��Ƶ�Ĵ��ھ��
    private Handler handler = new Handler();

    private Send encode;      //������
    private Receive decode;   //������
    private boolean isRunning; //�߳����б�־

    private RtpSocket rtp_socket = null; //����RTP�׽���

    private RtpPacket rtp_send_packet = null; //����RTP���Ͱ�
    private RtpPacket rtp_receive_packet = null; //����RTP���ܰ�


    //���� ����
    private long decoder_handle = 0; //ƴ֡���ľ��
    private byte[] frmbuf = new byte[65536]; //֡����
    private byte[] socket_receive_Buffer = new byte[2048]; //������
    private byte[] buffer = new byte[2048];

    //����
    private long encoder_handle = -1; //�����������ľ��
    private int send_packetNum = 0; //������Ŀ
    private int[] send_packetSize = new int[200]; //���ĳߴ�
    private byte[] send_stream = new byte[65536]; //����
    private byte[] socket_send_Buffer = new byte[65536]; //���� stream->socketBuffer->rtp_socket

    private String remote_ip;
    private int remote_port;

    //�ԳƼ��ܹ���
    SM4Utils sm=new SM4Utils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_view);
        remote_ip = getIntent().getStringExtra("remote_ip");
        remote_port=getIntent().getIntExtra("remote_port",8080);
        initView();

    }

    private void initView() {
    	topTitle=(TextView) findViewById(R.id.toptextView);
		topTitle.setText(getIntent().getStringExtra("name"));
        view = (VideoPlayView) this.findViewById(R.id.video_play);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //ʵ�����������
        encode = new Send();
        decode = new Receive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStart();
            }
        }, 1000);

    }

    /**
     * ���� ���� ����rtp�߳�  ������������ͷ
     */
    public void doStart() {

        //��ʼ��������
        if (rtp_socket == null) {
            try {
                //rtp_socket = new RtpSocket(new SipdroidSocket(20000)); //��ʼ���׽��֣�20000Ϊ���ն˿ں�
                rtp_socket = new RtpSocket(new SipdroidSocket(19888), InetAddress.getByName(remote_ip), remote_port);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            //��ʼ�����ܰ�
            rtp_receive_packet = new RtpPacket(socket_receive_Buffer, 0); //��ʼ�� ,socketBuffer�ı�ʱrtp_PacketҲ���Ÿı�
            /**
             * ��Ϊ���ܴ������ݹ��� �Ὣһ�����ݷָ�ɺü���������
             * ���ܷ� �������кźͽ����� ������Щ����ƴ�ӳ���������
             */
            //��ʼ��������
            decoder_handle = decode.CreateH264Packer(); //����ƴ֡��
            decode.CreateDecoder(352, 288); //����������
            isRunning = true;
            DecoderThread decoder = new DecoderThread();
            decoder.start(); //����һ���߳�

            //��ʼ�����Ͱ�
            rtp_send_packet = new RtpPacket(socket_send_Buffer, 0);
        }
        //��ʼ��������
        if (encoder_handle == -1)
            encoder_handle = encode.CreateEncoder(352, 288); //���õײ㺯��������������


        if (mCamera == null) {

            //����ͷ���ã�Ԥ����Ƶ
            mCamera = Camera.open(1); //ʵ��������ͷ�����  0Ϊ���� 1Ϊǰ��
            Camera.Parameters p = mCamera.getParameters(); //������ͷ��������p��
            p.setFlashMode("off");
            p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //p.setPreviewFormat(PixelFormat.YCbCr_420_SP); //����Ԥ����Ƶ�ĸ�ʽ
            p.setPreviewFormat(ImageFormat.NV21);
            p.setPreviewSize(352, 288); //����Ԥ����Ƶ�ĳߴ磬CIF��ʽ352��288
            //p.setPreviewSize(800, 600);
            p.setPreviewFrameRate(15); //����Ԥ����֡�ʣ�15֡/��
            mCamera.setParameters(p); //���ò���
            byte[] rawBuf = new byte[1400];
            mCamera.addCallbackBuffer(rawBuf);
            mCamera.setDisplayOrientation(90); //��Ƶ��ת90��
            try {
                mCamera.setPreviewDisplay(holder); //Ԥ������Ƶ��ʾ��ָ������
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview(); //��ʼԤ��

            //��ȡ֡
            //Ԥ���Ļص������ڿ�ʼԤ����ʱ�����жϷ�ʽ�����ã�ÿ�����15�Σ��ص�������Ԥ����ͬʱ�������ڲ��ŵ�֡
            Callback a = new Callback();
            mCamera.setPreviewCallback(a);
        }
    }



    //mCamera�ص�����
    class Callback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] frame, Camera camera) {


            if (encoder_handle != -1) {
                //�ײ㺯�������ذ�����Ŀ�����ذ��Ĵ�С�洢������packetSize�У�����������stream��
                send_packetNum = encode.EncoderOneFrame(encoder_handle, -1, frame, send_stream, send_packetSize);
                Log.d("log", "ԭʼ���ݴ�С��" + frame.length + "  ת������ݴ�С��" + send_stream.length);
                if (send_packetNum > 0) {

                    //ͨ��RTPЭ�鷢��֡
                    final int[] pos = {0}; //������ͷ����ʼȡ
                    final long timestamp = System.currentTimeMillis(); //�趨ʱ���
                    /**
                     * ��Ϊ���ܴ������ݹ��� �Ὣһ�����ݷָ�ɺü���������
                     * ���ܷ� �������кźͽ����� ������Щ����ƴ�ӳ���������
                     */
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int sequence = 0; //��ʼ�����к�
                            for (int i = 0; i < send_packetNum; i++) {

                                rtp_send_packet.setPayloadType(2);//���帺�����ͣ���ƵΪ2
                                rtp_send_packet.setMarker(i == send_packetNum - 1 ? true : false); //�Ƿ������һ��RTP��
                                rtp_send_packet.setSequenceNumber(sequence++); //���к����μ�1
                                rtp_send_packet.setTimestamp(timestamp); //ʱ���
                                //Log.d("log", "���к�:" + sequence + " ʱ�䣺" + timestamp);
                                rtp_send_packet.setPayloadLength(send_packetSize[i]); //���ĳ��ȣ�packetSize[i]+ͷ�ļ�

                                //������stream��pos����ʼ���ƣ���socketBuffer�ĵ�12���ֽڿ�ʼճ����packetSizeΪճ���ĳ���
                                System.arraycopy(send_stream, pos[0], socket_send_Buffer, 12, send_packetSize[i]); //��һ��������socketBuffer��
                                pos[0] += send_packetSize[i]; //�ض����´ο�ʼ���Ƶ�λ��
                                //rtp_packet.setPayload(socketBuffer, rtp_packet.getLength());
                                //  Log.d("log", "���к�:" + sequence + " bMark:" + rtp_packet.hasMarker() + " packetSize:" + packetSize[i] + " tPayloadType:2" + " timestamp:" + timestamp);

                                //����  ��Ҫ���͵����ݽ��м��� Ȼ���ͳ�ȥ
                                byte data[]=new byte[rtp_send_packet.packet_len];
                                System.arraycopy(rtp_send_packet.packet, 0, data, 0, rtp_send_packet.packet_len);
                                data= sm.encryptData_ECB(data);


                                try {
                                    rtp_socket.send(data);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }).start();


                }
            }
        }
    }

    /**
     * ����rtp���ݲ����� �߳�
     */
    class DecoderThread extends Thread {
        public void run() {
            while (isRunning) {
                byte[] data = new byte[2048]; //������
                int dataLength=0;
                try {
                    // rtp_socket.receive(rtp_receive_packet); //����һ����

                    //���ܼ�������  �����ǽ��ܵ���Ч���ݳ���
                    dataLength = rtp_socket.receive(data);
                } catch (IOException e) {
                	finish();
                	return;
                }

                //���ܲ���
                //���õ�����Ч���ݽ��н���
                data= sm.decryptData_ECB(Arrays.copyOf(data,dataLength));
                //���������ݷ���rtp_receive_packet��
                System.arraycopy(data,  0, socket_receive_Buffer, 0, data.length);
                rtp_receive_packet.packet_len = data.length;


                int packetSize = rtp_receive_packet.getPayloadLength(); //��ȡ���Ĵ�С
                Log.e("log","dataLength��"+dataLength+" packetSize:"+rtp_receive_packet.getLength());
                if (packetSize <= 0)
                    continue;
                if (rtp_receive_packet.getPayloadType() != 2) //ȷ�ϸ�������Ϊ2
                    continue;
                System.arraycopy(socket_receive_Buffer, 12, buffer, 0, packetSize); //socketBuffer->buffer
                int sequence = rtp_receive_packet.getSequenceNumber(); //��ȡ���к�
                long timestamp = rtp_receive_packet.getTimestamp(); //��ȡʱ���
                int bMark = rtp_receive_packet.hasMarker() == true ? 1 : 0; //�Ƿ������һ����


                int frmSize = decode.PackH264Frame(decoder_handle, buffer, packetSize, bMark, (int) timestamp, sequence, frmbuf); //packer=ƴ֡����frmbuf=֡����
                Log.d("log", "���к�:" + sequence + " bMark:" + bMark + " packetSize:" + packetSize + " PayloadType:" + rtp_receive_packet.getPayloadType() + " timestamp:" + timestamp + " frmSize:" + frmSize);
                if (frmSize <= 0)
                    continue;

                decode.DecoderNal(frmbuf, frmSize, view.mPixel);//������ͼ�����mPixel��

                //Log.d("log","���к�:"+sequence+" ����С��"+packetSize+" ʱ�䣺"+timestamp+"  frmbuf[30]:"+frmbuf[30]);
                view.postInvalidate();
            }

            //�ر�
            if (decoder_handle != 0) {
                decode.DestroyH264Packer(decoder_handle);
                decoder_handle = 0;
            }
            if (rtp_socket != null) {
                rtp_socket.close();
                rtp_socket = null;
            }
            decode.DestoryDecoder();
        }
    }

    /**
     * �ر�����ͷ ���ͷ���Դ
     */
    public void close() {
        isRunning = false;
        //�ͷ�����ͷ��Դ
        if (mCamera != null) {
            mCamera.setPreviewCallback(null); //ֹͣ�ص�����
            mCamera.stopPreview(); //ֹͣԤ��
            mCamera.release(); //�ͷ���Դ
            mCamera = null; //���³�ʼ��
        }

        if (rtp_socket != null) {
            rtp_socket.close();
            rtp_socket = null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			finish();
		}
		return true;
	}
}

package com.nercms.send;

/**
 *  H264 ����
 * Created by zsg on 2016/6/3.
 */

public class Send  {
    //�������ļ�
    static {
        System.loadLibrary("VideoEncoder");
    }

    //�ӿں���
    public native long CreateEncoder(int width, int height); //�ײ㴴�������������ر�����

    //����һ֡ͼ�񣬷��ذ�����Ŀ
    //type=����֡�����ͣ�frame=ԭʼyuvͼ��stream=ԭʼͼ��������packetSize=���ĳߴ�
    public native int EncoderOneFrame(long encoder, int type, byte[] frame, byte[] stream, int[] packetSize);

    public native int DestroyEncoder(long encoder); //���ٱ��������ͷ���Դ



}
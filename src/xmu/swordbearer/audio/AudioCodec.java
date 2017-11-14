package xmu.swordbearer.audio;

import android.util.Log;

public class AudioCodec {  //����jnj�еĿ�

	static {
		System.loadLibrary("audiowrapper");
		Log.e("AudioCodec", " audioWraper��������");
	}

	// initialize decoder and encoder
	public static native int audio_codec_init(int mode);  //��ʼ��

	// encode  
	public static native int audio_encode(byte[] sample, int sampleOffset,  //��Ƶ����
			int sampleLength, byte[] data, int dataOffset);

	// decode
	public static native int audio_decode(byte[] data, int dataOffset,  //��Ƶ�Ľ���
			int dataLength, byte[] sample, int sampleLength);
}

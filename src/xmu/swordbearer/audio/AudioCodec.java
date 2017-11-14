package xmu.swordbearer.audio;

import android.util.Log;

public class AudioCodec {  //调用jnj中的库

	static {
		System.loadLibrary("audiowrapper");
		Log.e("AudioCodec", " audioWraper库加载完毕");
	}

	// initialize decoder and encoder
	public static native int audio_codec_init(int mode);  //初始化

	// encode  
	public static native int audio_encode(byte[] sample, int sampleOffset,  //音频编码
			int sampleLength, byte[] data, int dataOffset);

	// decode
	public static native int audio_decode(byte[] data, int dataOffset,  //音频的解码
			int dataLength, byte[] sample, int sampleLength);
}

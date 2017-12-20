package com.lzk.gdut.audio.sm;




public class SM4Utils
{
	public static String secretKey ="asasasasasasasas";
	public boolean hexString = false;
	public static boolean isEntryptVoice = false;
	
	

	public void setEntryptVoice(boolean isEntryptVoice) {
		SM4Utils.isEntryptVoice = isEntryptVoice;
	}
	
	public boolean getEntryptVoice() {
		return isEntryptVoice;
	}

	public static void setSecretKey(String secretKey) {
		SM4Utils.secretKey = secretKey;
	}

	public SM4Utils()
	{
	}
	
	public byte[] encryptData_ECB(byte[] plainText)
	{
		try 
		{
			SM4_Context ctx = new SM4_Context();
			ctx.isPadding = true;
			ctx.mode = SM4.SM4_ENCRYPT;
			
			byte[] keyBytes;
			if (hexString)
			{
				keyBytes = Util.hexStringToBytes(secretKey);
			}
			else
			{
				keyBytes = secretKey.getBytes();
			}
			
			SM4 sm4 = new SM4();
			sm4.sm4_setkey_enc(ctx, keyBytes);
			byte[] cipherText = sm4.sm4_crypt_ecb(ctx, plainText);
			
			return cipherText;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public byte[] decryptData_ECB(byte[] cipherText)
	{
		try 
		{
			SM4_Context ctx = new SM4_Context();
			ctx.isPadding = true;
			ctx.mode = SM4.SM4_DECRYPT;
			
			byte[] keyBytes;
			if (hexString)
			{
				keyBytes = Util.hexStringToBytes(secretKey);
			}
			else
			{
				keyBytes = secretKey.getBytes();
			}
			
			SM4 sm4 = new SM4();
			sm4.sm4_setkey_dec(ctx, keyBytes);
			byte[] decrypted = sm4.sm4_crypt_ecb(ctx, cipherText);
			return decrypted;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return null;
		}
	}
		
	
}

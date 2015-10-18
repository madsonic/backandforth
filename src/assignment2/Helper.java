package assignment2;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.zip.CRC32;

public class Helper {
	public static final int pktSize = 1000;
	public static final int checksumLen = 8;
	public static final int seqNumLen = 8;
	public static final int ackLen = 1;
	public static final int finLen = 1;
	public static final int initLen = 1;
	public static final int fileSizeLen = 8;
	public static final int filenameLen = 255;
	
//	public static final int numSegLen = 8;
	public static final int headerSize = checksumLen + initLen + seqNumLen + finLen;
	public static final int dataSize = pktSize - headerSize;
	public static final int infoPktSize = checksumLen + ackLen + seqNumLen;
	public static final byte yesByte = 1;
	public static final byte noByte = 0;
	
	public static final int charLen = 255;
	public static final Charset charset = Charset.forName("UTF-8");
//	public static final CharsetEncoder encoder = charset.newEncoder();
//	public static final CharsetDecoder decoder = charset.newDecoder();
	
	
	public static long makeCheckSum(byte[] data) {
		assert data.length > checksumLen;
		CRC32 crc32 = new CRC32();
		crc32.update(data, checksumLen, data.length - checksumLen);
		return crc32.getValue();
	}
	
	public static boolean isCorrupt(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		long pktChksum = b.getLong();
		
		long calcPktChksum = makeCheckSum(data);
		
//		System.out.println(calcPktChksum);
//		System.out.println(pktChksum);
		
		if (pktChksum != calcPktChksum) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isNak(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		byte ackFlag = b.get(checksumLen);
		
		if (ackFlag == yesByte) {
			return false;
		} else {
			return true;
		}
	}
	
	public static boolean isFin(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		byte finFlag = b.get(checksumLen + initLen + seqNumLen);
		
		if (finFlag == yesByte) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isInit(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		byte initFlag = b.get(checksumLen);
		
		if (initFlag == yesByte) {
			return true;
		} else {
			return false;
		}
	}
	
	public static long getSeqNum(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		long num = b.getLong(checksumLen + initLen);
		
		return num;
	}
	
	public static long getInfoSeqNum(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		long num = b.getLong(checksumLen + initLen);
		
		return num;
	}

	public static long getFileSize(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		long size = b.getLong(checksumLen + initLen + seqNumLen);
		
		return size;
	}
	
	public static String getFilename(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		byte[] str = new byte[filenameLen];
		ByteBuffer b = ByteBuffer.wrap(data);
		ByteBuffer strBuffer = ByteBuffer.wrap(str);
		
		b.position(checksumLen + initLen + seqNumLen + fileSizeLen);
		b.get(str, 0, filenameLen);
		
		return byteBuffToStr(strBuffer); 
	}		

	public static ByteBuffer strToByteBuff(String s) {
		byte[] data = new byte[s.length()];
		ByteBuffer b = ByteBuffer.wrap(data);
		b = charset.encode(s);
		
		return b;
	}
	
	public static String byteBuffToStr(ByteBuffer b) {
		return charset.decode(b).toString().trim();
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    System.out.println(hexChars.length);
	    return new String(hexChars);
	}
}

package util;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Helper {
	public static final int pktSize = 1000;
	public static final int checksumLen = 8;
	public static final int seqNumLen = 8;
	public static final int ackLen = 1;
	public static final int finLen = 1;
	public static final int initLen = 1;
	public static final int fileSizeLen = 8;
	public static final int numSegLen = 8;
	public static final int headerSize = checksumLen + seqNumLen + finLen + initLen + fileSizeLen + numSegLen;
	public static final int dataSize = pktSize - headerSize;
	public static final int infoPktSize = checksumLen + ackLen;
	public static final byte yesByte = 1;
	public static final byte noByte = 0;
	
	
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
		byte finFlag = b.get(checksumLen + seqNumLen);
		
		if (finFlag == yesByte) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isInit(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		byte initFlag = b.get(checksumLen + seqNumLen + finLen);
		
		if (initFlag == yesByte) {
			return true;
		} else {
			return false;
		}
	}
	
	public static long getSeqNum(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		long num = b.getLong(checksumLen);
		
		return num;
	}

	public static long getFileSize(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		long size = b.getLong(checksumLen + seqNumLen + finLen + initLen);
		
		return size;
	}
	
	public static long getNumSeg(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		ByteBuffer b = ByteBuffer.wrap(data);
		long num = b.getLong(checksumLen + seqNumLen + finLen + initLen + fileSizeLen);
		
		return num;
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

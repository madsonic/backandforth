package util;

import static util.Helper.ackPktSize;
import static util.Helper.makeCheckSum;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Helper {
	public static final int arraySize = 1000;
	public static final int checksumLen = 8;
	public static final int headerSize = checksumLen;
	public static final int ackPktSize = checksumLen + 1;
	public static final byte ackbyte = 0x1;
	public static final byte nakbyte = 0x0;
	
	
	public static long makeCheckSum(byte[] data) {
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
		
		if (ackFlag == ackbyte) {
			return false;
		} else if (ackFlag == nakbyte) {
			return true;
		} else {
			assert false;
			return false;
		}
	}
	
	public static void sendAck(DatagramSocket socket, DatagramPacket pkt) {
		sendAckPkt(socket, pkt, true);
	}
	
	public static void sendNak(DatagramSocket socket, DatagramPacket pkt) {
		sendAckPkt(socket, pkt, false);
	}
	
	/**
	 * Makes a ack packet
	 * @param socket
	 * @param pkt
	 * @param ack ack if true nak if false
	 */
	private static void sendAckPkt(DatagramSocket socket, DatagramPacket pkt, boolean ack) {
		byte[] ackMsg = new byte[ackPktSize];
		ByteBuffer b = ByteBuffer.wrap(ackMsg);
		byte ackFlag;
		if (ack) {
			ackFlag = ackbyte;
		} else {
			ackFlag = nakbyte;
		}
		b.putLong(0);
		b.put(ackFlag);
		
		b.rewind();
		b.putLong(makeCheckSum(ackMsg));
		
		DatagramPacket ackPkt = 
				new DatagramPacket(ackMsg, ackMsg.length, 
								   pkt.getAddress(), pkt.getPort());
		try {
			socket.send(ackPkt);
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}

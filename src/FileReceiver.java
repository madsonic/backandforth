import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;

public class FileReceiver {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: SimpleUDPReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		DatagramSocket socket = new DatagramSocket(port);
		byte[] data = new byte[1500];
		DatagramPacket pkt = new DatagramPacket(data, data.length);
		ByteBuffer b = ByteBuffer.wrap(data);
		FileOutputStream out = new FileOutputStream(new File("test.txt"));
		System.out.println("Listening on port: " + port);
		
		while(true) {
			pkt.setLength(data.length);
			socket.receive(pkt);
			if (pkt.getLength() < 8) {
				System.out.println("Pkt too short");
				continue;
			}
			// Debug output
			// System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			out.write(pkt.getData());
			out.flush();
			
			byte[] ackMsg = "Received".getBytes();
			DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length, pkt.getAddress(), pkt.getPort());
			socket.send(ack);
		}
		
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int len) {
	    char[] hexChars = new char[len * 2];
	    for ( int j = 0; j < len; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}

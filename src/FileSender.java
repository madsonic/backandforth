import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.*;

public class FileSender {

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <path>");
			System.exit(-1);
		}
		
		// Setup from inputs
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		Path path = Paths.get(args[2]);
		
		// send
		InetSocketAddress addr = new InetSocketAddress(host, port);
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket p = makePacket(path, addr);
		assert p != null; // pkt cannot be empty
		socket.send(p);
		
		// listen to ack 
		byte[] outData = new byte[1024];
		DatagramPacket inPacket = new DatagramPacket(outData, outData.length);
		socket.receive(inPacket);
		String ack = new String(inPacket.getData());
		System.out.println(ack);
	}
	
	private static DatagramPacket makePacket(Path path, InetSocketAddress addr) {
		DatagramPacket pkt;
		byte[] data = new byte[20];
		try {
			data = Files.readAllBytes(path);
			pkt = new DatagramPacket(data, data.length, addr);
			return pkt;
		} catch (IOException e) {
			System.out.println(e);
		}
		return null;
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

package assignment2;

import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static util.Helper.*;

public class FileSender {

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <path>");
			System.exit(-1);
		}
		
		// Setup from inputs
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		Path path = Paths.get(args[2]);
		DatagramSocket socket = null;
		
		try {
			// send
			InetSocketAddress addr = new InetSocketAddress(host, port);
			socket = new DatagramSocket();
			DatagramPacket p = makePacket(path, addr);
			assert p != null; // pkt cannot be empty
			socket.send(p);
			
			// listen to ack
			// need to check ack packet corruptibility
			byte[] ack = new byte[ackPktSize];
			DatagramPacket ackPkt = new DatagramPacket(ack, ack.length);
			ByteBuffer b = ByteBuffer.wrap(ack);
			
			while (true) {
				socket.receive(ackPkt);
				
				// resend if ack is corrupted/nak/timeout
				if (isCorrupt(ackPkt) || isNak(ackPkt)) {
					System.out.println("resending");
					socket.send(p);
				} else {
					// packet succesfully sent and acknowledged
					System.out.println("acknowledged");
					break;
				}
			} 
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) socket.close();
		}
	}
	
	private static DatagramPacket makePacket(Path path, InetSocketAddress addr) {
		DatagramPacket pkt;
		byte[] data = new byte[arraySize];
		ByteBuffer b = ByteBuffer.wrap(data);
		FileChannel chnl = null;
		
		try {
			b.clear();
			b.putLong(0); // reserve for checksum
			
			chnl = FileChannel.open(path, StandardOpenOption.READ);
			// put in data
			chnl.read(b);
			
			// put in checksum
			b.rewind();
			long checkSum = makeCheckSum(data);
			b.putLong(checkSum);
			
			pkt = new DatagramPacket(data, data.length, addr);
			return pkt;
		} catch (IOException e) {
			System.out.println(e);
		} finally {
			try {
				if (chnl != null) chnl.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}

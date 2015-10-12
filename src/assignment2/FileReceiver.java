package assignment2;

import static util.Helper.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static util.Helper.*;

public class FileReceiver {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: SimpleUDPReceiver <port>");
			System.exit(-1);
		}
		
		// Set up variables
		int port = Integer.parseInt(args[0]);
		FileChannel out = null;
		DatagramSocket socket = null;
		
		try {
			socket = new DatagramSocket(port);
			byte[] data = new byte[arraySize];
			DatagramPacket pkt = new DatagramPacket(data, data.length);
			ByteBuffer b = ByteBuffer.wrap(data);
			
			out = FileChannel.open(Paths.get("test.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			
			System.out.println("Listening on port: " + port);
			
			while(true) {
				socket.receive(pkt);
				
				// Check packet length
				if (pkt.getLength() < 8) {
					System.out.println("Pkt too short");
					continue;
				}
				
				// Check packet corruption
				if (isCorrupt(pkt)) {
					System.out.println("sending NAK");
					sendNak(socket, pkt);
				} else {
					// set position after header so that only data is written out
					b.position(headerSize);
					out.write(b);
					out.force(false);
					
					System.out.println("sending ACK");
					sendAck(socket, pkt);
				}
				// Debug output
//			 System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
				
				
				
//			byte[] ackMsg = "Received".getBytes();
//			DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length, pkt.getAddress(), pkt.getPort());
//			socket.send(ack);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) out.close();
				if (socket != null) socket.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

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
import java.util.HashSet;

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
		HashSet<Long> set = new HashSet<Long>();
		long numSegments = 1;
		long fileSize = dataSize;
		
		try {
			socket = new DatagramSocket(port);
			byte[] data = new byte[pktSize];
			DatagramPacket pkt = new DatagramPacket(data, data.length);
			ByteBuffer b = ByteBuffer.wrap(data);
			
			out = FileChannel.open(Paths.get("test.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			
			System.out.println("Listening on port: " + port);
			
			for (long i = -1; i < numSegments; ++i) {
				socket.receive(pkt);
				long seqNum = getSeqNum(pkt);
				
				if (isCorrupt(pkt)) {
					System.out.println("corrupt packet");
					sendNak(socket, pkt);
					--i;
				} else if (set.contains(seqNum)) {
					System.out.println("dropping duplicate packet");
					sendAck(socket, pkt);
					--i;
					continue;
				} else if (isInit(pkt)) {
					// it's a init packet
					System.out.println("Recv init packet");
					fileSize = getFileSize(pkt);
					numSegments = getNumSeg(pkt);
					sendAck(socket, pkt);
					set.add(seqNum);
				} else if (isFin(pkt)) {
					set.add(seqNum);
					// set position after header so that only data is written out
					b.position(headerSize);
					out.write(b, seqNum);
					out.force(true);
					out.truncate(fileSize);
					
					sendAck(socket, pkt);
					System.out.println("Rcv fin packet");
					
					System.out.println("Finished");
				} else {
					set.add(seqNum);
					// set position after header so that only data is written out
					b.position(headerSize);
					out.write(b, seqNum);
					out.force(true);
					
					System.out.println("Rcv normal packet");
					sendAck(socket, pkt);
				}
				
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				System.out.println("closing");
				if (out != null) out.close();
				if (socket != null) socket.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void sendAck(DatagramSocket socket, DatagramPacket pkt) {
		sendInfoPkt(socket, pkt, true);
	}
	
	public static void sendNak(DatagramSocket socket, DatagramPacket pkt) {
		sendInfoPkt(socket, pkt, false);
	}
	
	/**
	 * Makes a ack packet
	 * @param socket
	 * @param pkt
	 * @param ack ack if true nak if false
	 */
	private static void sendInfoPkt(DatagramSocket socket, DatagramPacket pkt, boolean ack) {
		byte ackFlag;
		byte[] infoMsg = new byte[infoPktSize];
		
		if (ack) {
			ackFlag = yesByte;
		} else {
			ackFlag = noByte;
		}
		
		ByteBuffer b = ByteBuffer.wrap(infoMsg);
		
		b.putLong(0); // checksum
		b.put(ackFlag);
		
		b.rewind();
		b.putLong(makeCheckSum(infoMsg));
		DatagramPacket ackPkt = 
				new DatagramPacket(infoMsg, infoMsg.length, 
								   pkt.getAddress(), pkt.getPort());
		try {
			socket.send(ackPkt);
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

package assignment2;

import static assignment2.Helper.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

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
		HashSet<Long> seqNumSet = new HashSet<Long>();
		long fileSize = dataSize;
		long seqNum = -dataSize;
		
		try {
			socket = new DatagramSocket(port);
			byte[] data = new byte[pktSize];
			DatagramPacket pkt = new DatagramPacket(data, data.length);
			ByteBuffer b = ByteBuffer.wrap(data);
			
			
			System.out.println("Listening on port: " + port);
			
			while (true) {
				socket.receive(pkt);
				System.out.println("seq #" + seqNum);
				
				if (isCorrupt(pkt)) {
					System.out.println("corrupt packet");
					
					sendNak(socket, pkt, seqNum);
					continue;
				} 
				seqNum = getSeqNum(pkt);
				
				if (seqNumSet.contains(seqNum)) {
					System.out.println("dropping duplicate packet");
					
					sendAck(socket, pkt, seqNum);
					continue;
				} else if (isInit(pkt)) {
					// it's a init packet
					System.out.println("Rcv init packet");
					
					fileSize = getFileSize(pkt);
					System.out.println("filename " + getFilename(pkt));
					out = FileChannel.open(Paths.get(getFilename(pkt)), 
										   StandardOpenOption.CREATE, 
										   StandardOpenOption.WRITE,
										   StandardOpenOption.SYNC);
					
					sendAck(socket, pkt, seqNum);
					seqNumSet.add(seqNum);
				} else {
					// set position after header so that only data is written out
					System.out.println("Rcv normal packet");
					b.position(headerSize);
					out.write(b, seqNum);
					out.force(true);
					if (isFin(pkt)) { out.truncate(fileSize); }
					
					sendAck(socket, pkt, seqNum);
					seqNumSet.add(seqNum);
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
	
	public static void sendAck(DatagramSocket socket, DatagramPacket pkt, long seqNum) {
		sendInfoPkt(socket, pkt, true, seqNum);
	}
	
	public static void sendNak(DatagramSocket socket, DatagramPacket pkt, long seqNum) {
		sendInfoPkt(socket, pkt, false, seqNum);
	}
	
	/**
	 * Makes a ack packet
	 * @param socket
	 * @param pkt
	 * @param ack ack if true nak if false
	 */
	private static void sendInfoPkt(DatagramSocket socket, DatagramPacket pkt, boolean ack, long seqNum) {
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
		b.putLong(seqNum);
		
		b.rewind();
		b.putLong(makeCheckSum(infoMsg));
		DatagramPacket ackPkt = 
				new DatagramPacket(infoMsg, infoMsg.length, pkt.getAddress(), pkt.getPort());
		try {
			socket.send(ackPkt);
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

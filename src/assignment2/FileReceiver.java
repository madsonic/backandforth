package assignment2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
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
		FileChannel fc = null;
		MappedByteBuffer out = null;
		DatagramSocket socket = null;
		HashSet<Long> seqNumSet = new HashSet<Long>();
		long fileSize = Helper.dataSize;
		long seqNum = -Helper.dataSize;
		
		try {
			socket = new DatagramSocket(port);
			byte[] data = new byte[Helper.pktSize];
			DatagramPacket pkt = new DatagramPacket(data, data.length);
			ByteBuffer b = ByteBuffer.wrap(data);
			
			
			System.out.println("Listening on port: " + port);
			
			while (true) {
				socket.receive(pkt);
				System.out.println("seq #" + seqNum);
				
				if (Helper.isCorrupt(pkt)) {
					System.out.println("corrupt packet");
					
					sendNak(socket, pkt, seqNum);
					continue;
				} 
				seqNum = Helper.getSeqNum(pkt);
				
				if (seqNumSet.contains(seqNum)) {
					System.out.println("dropping duplicate packet");
					
					sendAck(socket, pkt, seqNum);
					continue;
				} else if (Helper.isInit(pkt)) {
					// it's a init packet
					System.out.println("Rcv init packet");
					
					fileSize = Helper.getFileSize(pkt);
					System.out.println("filename " + Helper.getFilename(pkt));
					fc = FileChannel.open(Paths.get(Helper.getFilename(pkt)), 
							StandardOpenOption.CREATE, 
							StandardOpenOption.WRITE,
							StandardOpenOption.READ);
					
					sendAck(socket, pkt, seqNum);
					seqNumSet.add(seqNum);
				} else {
					// set position after header so that only data is written out
					System.out.println("Rcv normal packet");
					b.position(Helper.headerSize);
					
					if (fileSize > Helper.fileSizeLimit) {
						out = fc.map(MapMode.READ_WRITE, seqNum, Helper.dataSize);
						out.put(b);
					} else {
						fc.write(b, seqNum);
						fc.force(true);
					}
					
					if (Helper.isFin(pkt)) { fc.truncate(fileSize); }
					
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
				if (fc != null) fc.close();
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
		byte[] infoMsg = new byte[Helper.infoPktSize];
		
		ackFlag = ack ? Helper.yesByte : Helper.noByte;
		
		ByteBuffer b = ByteBuffer.wrap(infoMsg);
		
		b.putLong(0); // checksum
		b.put(ackFlag);
		b.putLong(seqNum);
		
		b.rewind();
		b.putLong(Helper.makeCheckSum(infoMsg));
		DatagramPacket ackPkt = 
				new DatagramPacket(infoMsg, infoMsg.length, pkt.getAddress(), pkt.getPort());
		try {
			socket.send(ackPkt);
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

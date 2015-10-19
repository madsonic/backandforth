package assignment2;

import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileSender {
	public static final int WAIT_TIME = 5; //////////////////////////////////////////////////////////////// CHANGE THIS BEFORE SUBMISSION

	public static void main(String[] args) {
		if (args.length != 4) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <path> <filename>");
			System.exit(-1);
		}
		
		StopWatch watch = new StopWatch();
		watch.start();
		
		// Setup from inputs
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		Path path = Paths.get(args[2]);
		String filename = args[3];
		
		DatagramSocket socket = null;
		FileChannel chnl = null;
		InetSocketAddress addr = null;
		byte[] ack = null;
		DatagramPacket ackPkt = null;
		long seqNum;
		long rcvSeqNum;
		DatagramPacket p;
		
		try {
			chnl = FileChannel.open(path, StandardOpenOption.READ);
			long fileSize = chnl.size();
			long numSegments = (long)Math.ceil((double)fileSize / (double)Helper.dataSize);
//System.out.println(numSegments);
			addr = new InetSocketAddress(host, port);
			socket = new DatagramSocket();
			socket.setSoTimeout(WAIT_TIME);
			
			
			// Main delivery
			for (long i = -1; i < numSegments; ++i) {
				seqNum = i * Helper.dataSize;
//System.out.println("seq #" + seqNum);
				
				// make packets
				if (i < 0) {
					// send a info packet with last seg size and num seg
					// this packet must be delivered before the main delivery starts
//System.out.println("init packet");
					p = makeInitPacket(addr, seqNum, fileSize, filename);
				}  else {
//System.out.println("normal packet");
					p = makeDataPacket(chnl, addr, seqNum, i == numSegments - 1);
				}
				
				// send packets and receive acknowledgements
				while (true) {
					ack = new byte[Helper.infoPktSize];
					ackPkt = new DatagramPacket(ack, ack.length);
					socket.send(p);
					
					try {
						socket.receive(ackPkt);
						if (Helper.isCorrupt(ackPkt)) {
//System.out.println("corrupted");
							continue;
						}
						
						rcvSeqNum = Helper.getSeqNum(ackPkt);
						if (!Helper.isNak(ackPkt) && rcvSeqNum == seqNum) { break; }
						
					} catch (SocketTimeoutException timeoutException) {
//System.out.println("timeout");
					}
				}
					
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) socket.close();
			if (chnl != null)
				try {
//System.out.println("closing channel");
					chnl.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		watch.stop();
		System.out.println("Time taken: " + watch.getTime());
	}
	
	private static DatagramPacket makeInitPacket(InetSocketAddress addr, long seqNum, long fileSize, String filename) {
		DatagramPacket pkt = null;
		byte[] data = new byte[Helper.pktSize];
		ByteBuffer b = ByteBuffer.wrap(data);
		
		// reserve for headers
		b.putLong(0);                   // checksum
		b.put(Helper.yesByte);                 // init flag
		b.putLong(seqNum);              // seq num
		b.putLong(fileSize);            // file size
		b.put(Helper.strToByteBuff(filename)); // file name
		
		b.rewind();
		b.putLong(Helper.makeCheckSum(data));
		pkt = new DatagramPacket(data, data.length, addr);
		
		assert pkt != null;
		return pkt;
	}
	
	private static DatagramPacket makeDataPacket(FileChannel chnl, 
												InetSocketAddress addr, 
												long seqNum,
												boolean fin) {
		DatagramPacket pkt = null;
		byte[] data = new byte[Helper.pktSize];
		ByteBuffer b = ByteBuffer.wrap(data);
		
		try {
			// reserve for headers
			b.putLong(0);      // checksum
			b.put(Helper.noByte);     // init flag
			b.putLong(seqNum); // seq num
			
			if (fin) {
				b.put(Helper.yesByte);
			} else {
				b.put(Helper.noByte);
			}
			
			chnl.read(b);

			b.rewind();
			b.putLong(Helper.makeCheckSum(data));
			pkt = new DatagramPacket(data, data.length, addr);
			return pkt;
		} catch (IOException e) {
			System.out.println(e);
		} 
		assert pkt != null;
		return null;
	}
	
}
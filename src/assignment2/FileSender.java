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
		FileChannel chnl = null;
		InetSocketAddress addr = null;
		
		try {
			chnl = FileChannel.open(path, StandardOpenOption.READ);
			long fileSize = chnl.size();
			long numSegments = (long)Math.ceil((double)fileSize / (double)dataSize);
			addr = new InetSocketAddress(host, port);
			socket = new DatagramSocket();
			
			// Main delivery
			long seqNum;
			DatagramPacket p;
			
			for (long i = -1; i < numSegments; ++i) {
				seqNum = i * dataSize;
				
				if (i < 0) {
					// send a info packet with last seg size and num seg
					// this packet must be delivered before the main delivery starts
					System.out.println("sending init");
					p = makeInitPacket(addr, seqNum, fileSize, numSegments);
				} else if (i == numSegments - 1) {
					// add fin flag
					System.out.println("last packet");
					p = makeDataPacket(chnl, addr, seqNum, true);
				} else {
					System.out.println("normal packet");
					p = makeDataPacket(chnl, addr, seqNum, false);
				}
				
				assert p != null; // pkt cannot be empty
				socket.send(p);
				
				// listen to ack
				// need to check ack packet corruptibility
				byte[] ack = new byte[infoPktSize];
				DatagramPacket ackPkt = new DatagramPacket(ack, ack.length);
				
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
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) socket.close();
			if (chnl != null)
				try {
					System.out.println("closing channel");
					chnl.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	private static DatagramPacket makeInitPacket(InetSocketAddress addr, long seqNum, long fileSize, long numSeg) {
		return makeOutPacket(null, addr, seqNum, false, true, fileSize, numSeg, false);
	}
	
	private static DatagramPacket makeDataPacket(FileChannel chnl, 
												InetSocketAddress addr, 
												long seqNum,
												boolean fin) {
		return makeOutPacket(chnl, addr, seqNum, fin, false, 0, 0, true);
	}
	
	// Adds in all the necessary header
	private static DatagramPacket makeOutPacket(FileChannel chnl, InetSocketAddress addr, 
												long seqNum, boolean fin, boolean init,
												long fileSize, long numSeg, boolean isData) {
		DatagramPacket pkt = null;
		byte[] data = new byte[pktSize];
		ByteBuffer b = ByteBuffer.wrap(data);
		
		try {
			// reserve for headers
			b.putLong(0);      // checksum
			b.putLong(seqNum); // seq num
			
			if (fin) {
				b.put(yesByte);
			} else {
				b.put(noByte);
			}
			
			if (init) {
				b.put(yesByte);
			} else {
				b.put(noByte);
			}
			
			b.putLong(fileSize);
			b.putLong(numSeg);

			if (isData) {
				chnl.read(b);
			}

			b.rewind();
			b.putLong(makeCheckSum(data));
			pkt = new DatagramPacket(data, data.length, addr);
			return pkt;
		} catch (IOException e) {
			System.out.println(e);
		} 
		assert pkt != null;
		return null;
	}
}

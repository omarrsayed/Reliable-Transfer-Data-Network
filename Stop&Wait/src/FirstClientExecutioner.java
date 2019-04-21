import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

public class FirstClientExecutioner {
	
	public static final int DATAGRAM_SIZE = 1012;
	public static final int TIME_OUT = 3000; // 3000 milliseconds

	public static void main(String args[]) throws IOException {
		
		System.out.println("==================================================");
		System.out.println("====================== CLIENT ====================");
		System.out.println("==================================================\r\n");
		
		// initialize client
		Client client = new Client("clientinfo.txt");
		
		// print initial client and server information
		System.out.println("CLIENT ARGUMENTS:");
		System.out.println("-----------------");
		System.out.println("server_ip ---> " + client.serverAddress);
		System.out.println("server_port ---> " + client.listeningPort);
		System.out.println("client_ip ---> " + client.clientPort);
		System.out.println("file_name ---> " + client.fileName);	
		System.out.println("window_size ---> " + client.windowSize);
		
		client.requestFile();
		
		System.out.println("\r\nRequested file transfer from the server.");
		System.out.println("-----------------------------------------------------\r\n");
		
		// set timeout for the file request
		client.clientSocket.setSoTimeout(TIME_OUT);
		
		while(true) {
			
			try {
				
				// receive acknowledgement for the file request
				byte[] receiveDatagram = new byte[DATAGRAM_SIZE];
				DatagramPacket receivePacket = new DatagramPacket(receiveDatagram, receiveDatagram.length);
				client.clientSocket.receive(receivePacket);
				Packet ackFileRequestPacket = new Packet(receiveDatagram);
		
				if(ackFileRequestPacket.getLength() == 12) {
					break;
				}
				
			} catch(SocketTimeoutException e) {
				
				// request file again
				client.requestFile();
				
				System.out.println("Request file transfer request timeout");
				System.out.println("Resend file transfer request");
				System.out.println("-----------------------------------------------------\r\n");
				
			}
			
		}
		
		// disable timeout again
		client.clientSocket.setSoTimeout(0);
		
		System.out.println("File is successfully Request from the server.");
		System.out.println("-----------------------------------------------------\r\n");
		
		System.out.println("Receiving the file...");
		System.out.println("---------------------\r\n");
		
		// tracks total number of packets correctly received
		int receivedCount = 0;
		
		long startTime = System.currentTimeMillis(); 
		
		while(true) {
			
			Packet packet = client.receivePacket();
			int sequenceNumber = packet.getSeqno();
			
			if(client.isTerminatePacket(packet)) {
				break;
			}
			
			// check validity of checksum
			if(packet.isValid()) {
				
				System.out.println("RECEIVED  | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
				System.out.println("SENT ACK  | #" + sequenceNumber +  " | TO: " + client.host + "   | " + client.instancePort + " | AT: " + client.clientPort);
				
				client.appendData(packet);
				client.sendAcknowledgment(sequenceNumber);
				
				// increment packets received
				receivedCount++;
				
			} else {
				
				System.out.println("CORRUPTED | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
				System.out.println("IGNORE    | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
			
			}
			
		}
		
		long stopTime = System.currentTimeMillis();
	    long elapsedTime = (stopTime - startTime) / 1000;
	    
		System.out.println("\r\nThe file has been fully received.");
		System.out.println("Successfully received " + receivedCount + " packets.");
		System.out.println("Elapsed time in seconds = " + elapsedTime);
		System.out.println("--------------------------------");
		
		client.writeFile();
		client.clientSocket.close();
		
	}
	
}

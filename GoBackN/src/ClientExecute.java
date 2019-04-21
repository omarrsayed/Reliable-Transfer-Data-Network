import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

public class ClientExecute {
	
	public static final int ACK = 1;
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
		
		int deliveredPackets = 0; // tracks highest in-order sequence Number
		
		while(true) {
			
			Packet packet = client.receivePacket();
			
			if(client.isTerminatePacket(packet)) {
				break;
			}
			
			int sequenceNumber = packet.getSeqno();
			
			// check validity of sequence and checksum
			if(sequenceNumber == deliveredPackets) { 
				
				if(packet.isValid()) {
				
					System.out.println("RECEIVED   | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
					System.out.println("SENT ACK   | #" + sequenceNumber +  " | TO:   " + client.host + " | " + client.instancePort + " | FROM: " + client.clientPort);
					
					client.appendData(packet);
					client.sendAcknowledgment(sequenceNumber);
					
					deliveredPackets++;
				
				} else {
					
					System.out.println("CORRUPTED  | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
					System.out.println("IGNORE     | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
					
				}
				
			} else {
				
				System.out.println("RECEIVED   | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
				System.out.println("DISCARDED  | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
				System.out.println("RESENT ACK | #" + (deliveredPackets - 1) +  " | TO:   " + client.host + " | " + client.instancePort + " | FROM: " + client.clientPort);
			
				// re-send cumulative acknowledgement
				client.sendAcknowledgment(deliveredPackets - 1);
			
			}
			
		}
		
		System.out.println("\r\nThe file has been fully received.");
		System.out.println("Successfully received " + deliveredPackets + " packets.");
		System.out.println("--------------------------------");
		
		client.writeFile();
		client.clientSocket.close();
		
	}
	
}

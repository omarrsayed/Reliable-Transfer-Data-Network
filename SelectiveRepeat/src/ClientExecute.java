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
		
		// disable timeout again, 0 refers to infinite timeout
		client.clientSocket.setSoTimeout(0);
		
		System.out.println("File is successfully Request from the server.");
		System.out.println("-----------------------------------------------------\r\n");
		
		System.out.println("Receiving the file...");
		System.out.println("---------------------\r\n");
		
		int deliveredPackets = 0;
		int maxSequenceNumber = 0; // necessary for reallocating the buffer
		
		long startTime = System.currentTimeMillis(); 
		
		while(true) {
			
			Packet packet = client.receivePacket();
			
			if(client.isTerminatePacket(packet)) {
				break;
			}
			
			int sequenceNumber = packet.getSeqno();
			
			// check validity of sequence within window
			if(sequenceNumber >= client.base && sequenceNumber < client.base + client.windowSize) { 
				
				if(packet.isValid()) {
					
					// add any acknowledged packet to packets buffer to deliver them later
					client.packetsBuffer.add(packet);
				
					System.out.println("RECEIVED  | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
					System.out.println("SENT ACK  | #" + sequenceNumber +  " | TO: " + client.host + "   | " + client.instancePort + " | AT: " + client.clientPort);
					
					if(sequenceNumber > maxSequenceNumber)  // necessary for reallocation of buffer
						maxSequenceNumber = sequenceNumber;
					
					client.buffer = client.reallocateBuffer(client.buffer, maxSequenceNumber);
					client.acknowledgePacket(sequenceNumber);
					client.sendAcknowledgment(sequenceNumber);
					
					while(client.base < client.buffer.length && client.buffer[client.base] == ACK) {
						
						System.out.println("DELIVERED | #" + client.base + " |");
						
						Packet deliverPacket = client.retreivePacket(client.base);
						client.appendData(deliverPacket);
						
						client.base++;
						deliveredPackets++;
						
					}
					
				} else {
					
					System.out.println("CORRUPTED | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
					System.out.println("IGNORE    | #" + sequenceNumber + " | FROM: " + client.host + " | " + client.instancePort + " | AT: " + client.clientPort);
				}
				
			}
			
		}
		
		long stopTime = System.currentTimeMillis();
	    long elapsedTime = (stopTime - startTime) / 1000;
	    
		System.out.println("\r\nThe file has been fully received.");
		System.out.println("Successfully received " + deliveredPackets + " packets.");
		System.out.println("Elapsed time in seconds = " + elapsedTime);
		System.out.println("--------------------------------");
		
		client.writeFile();
		client.clientSocket.close();
		
	}
	
}

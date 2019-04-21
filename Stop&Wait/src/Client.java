import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Client {
	
	public static final double corruptProbability = 0.01;
	public static final int CHUNK_SIZE = 1000;
	public static final int DATAGRAM_SIZE = 1012;
	
	String serverAddress;
	int listeningPort;
	int instancePort;
	int clientPort;
	String fileName;
	int windowSize;
	
	InetAddress host;
	DatagramSocket clientSocket;
	
	byte[] corruption;
	String message;
	Random random;
	
	public Client(String clientinfo) throws IOException {
		
		// random array used for manipulation of data segment of the packet
		corruption = new byte[CHUNK_SIZE];
		random = new Random();
		random.nextBytes(corruption);
		
		message = new String();
		recordInfo(clientinfo);
		host = InetAddress.getByName(serverAddress);
		clientSocket = new DatagramSocket(clientPort);
		
	}
	
	public void recordInfo(String clientinfo) throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(clientinfo));
		
		serverAddress = reader.readLine();
		listeningPort = Integer.parseInt(reader.readLine());
		clientPort = Integer.parseInt(reader.readLine());
		fileName = reader.readLine();
		windowSize = Integer.parseInt(reader.readLine());
		
		reader.close();
		
	}
	
	public void requestFile() throws IOException {
		
		Packet packet =  new Packet(fileName.getBytes(), "File Request" ,0);
		byte[] sendDatagram = packet.getPacketSegment();
		DatagramPacket sendPacket = new DatagramPacket(sendDatagram, sendDatagram.length, host, listeningPort); 
		clientSocket.send(sendPacket);
		
	}
	
	public Packet receivePacket() throws IOException {
		
		// receive packet
		byte[] receiveDatagram = new byte[DATAGRAM_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveDatagram, receiveDatagram.length);
		clientSocket.receive(receivePacket);
		
		// retrieve server port that handles the request
		instancePort = receivePacket.getPort();
		
		// manipulate data segment of the packet with some probability
		if(random.nextFloat() <= corruptProbability) {
			System.arraycopy(corruption, 0, receiveDatagram, 12, CHUNK_SIZE);
		}
		
		// return Packet
		return new Packet(receiveDatagram);
		
	}
	
	public boolean isTerminatePacket(Packet packet) {
		
		if(packet.getLength() == 0) 
			return true;
		return false;
		
	}
	
	public void appendData(Packet packet) {
		
		String response = new String(packet.getDataBytes());
		message = message + response;
		
	}
	
	public void sendAcknowledgment(int sequenceNumber) throws IOException {
		
		Packet packet = new Packet(null, "acknowledgement", sequenceNumber);
		byte[] ackDatagram = packet.getPacketSegment();
		DatagramPacket ackPacket = new DatagramPacket(ackDatagram, ackDatagram.length, host, instancePort);
		clientSocket.send(ackPacket);
		
	}
	
	public void writeFile() throws IOException {
		
		BufferedWriter out = new BufferedWriter(new FileWriter("output.txt"));
		out.write(message);
		out.close();
		
	}
	
}

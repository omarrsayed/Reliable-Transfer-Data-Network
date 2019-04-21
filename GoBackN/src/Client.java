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
	
	// constants
	public static final double corruptProbability = 0.01;
	public static final int CHUNK_SIZE = 1000;
	public static final int DATAGRAM_SIZE = 1012;
	public static final int ACK = 1;
	
	// required initial information about server and client
	String serverAddress;
	int listeningPort;
	int clientPort;
	String fileName;
	int windowSize;
	
	InetAddress host;
	DatagramSocket clientSocket;
	
	int instancePort;
	String message;
	byte[] corruption;
	Random random;
	
	// buffer
	int[] buffer;
	int base = 0; // base pointer of the buffer
	int orderPointer = 0; // points to the sequence number of the not-yet received packet
	int bufferPointer; // in-order pointer, points to the next not-yet received packet in the buffer
	
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
		buffer = new int[1]; // let it initially be = 1;
		
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
		/*
		if(random.nextFloat() <= corruptProbability) {
			System.arraycopy(corruption, 0, receiveDatagram, 12, CHUNK_SIZE);
		}
		*/
		
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
	
	public void acknowledgePacket(int sequenceNumber) {
		
		buffer[sequenceNumber] = ACK;
		
	}
	
}

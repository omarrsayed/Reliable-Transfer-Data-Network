import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Handler implements Runnable {
	
	public static final int CHUNK_SIZE = 1000;
	public static final int DATAGRAM_SIZE = 1012;
	public static final int TIME_OUT = 800; // 800 milliseconds
	
	public int totalSize;
	public int totalPackets;
	public int packetCount;

	private Thread thread;
	private DatagramSocket serverSocket;
	private int port;
	private InetAddress address;
	private double lossProbability;
	private String filename;
	private int serverPort;
	private int seedValue;
	private Random random;
	private Queue <Packet> packets; // Queue of packets
	
	public Handler(int serverPort, String filename, double lossProbability, int seedValue, int clientPort, InetAddress clientAddress) throws SocketException {
		
		this.serverPort = serverPort;
		this.serverSocket = new DatagramSocket(serverPort);
		this.address = clientAddress;
		this.port = clientPort;
		this.filename = filename;
		this.lossProbability = lossProbability;
		this.seedValue = seedValue;
		packets = new LinkedList<Packet>();
		serverSocket.setSoTimeout(TIME_OUT);
		random = new Random((long) this.seedValue);
		
	}
	
	public void assignPackets(byte[] datapile) throws IOException {
		
		int sequenceNumber = 0;
		int start = 0;
		while(start < datapile.length) {
			int end = Math.min(datapile.length, start + CHUNK_SIZE);
			byte[] chunks = (Arrays.copyOfRange(datapile, start, end));
			start = start + CHUNK_SIZE;
			Packet sentPacket = new Packet(chunks, "datapacket", sequenceNumber);
			packets.add(sentPacket);
			sequenceNumber++;
		}
		
	}
	
	public void start() {
		
		thread = new Thread(this);
		thread.start();
		
	}
	
	public void processPackets() throws IOException {
		
		System.out.println("SERVER PORT #" + serverPort + " HANDLES THE FILE REQUEST \"" + filename + "\" FROM: " + address + " | " + port + ".");
		
		// read file
		Path filepath = Paths.get(filename);
		byte[] datapile = Files.readAllBytes(filepath);
		
		// assign overall size
		totalSize = datapile.length;
		
		// evaluate total number of packets
		packetCount = (int) Math.ceil((double)totalSize / CHUNK_SIZE);
		
		assignPackets(datapile);
		
		int currentCount = 0;
		
		while(!packets.isEmpty()) {
					
			// initialize packet
			Packet nextPacket = packets.remove();
			byte[] sendDatagram = nextPacket.getPacketSegment();
			DatagramPacket sendPacket = new DatagramPacket(sendDatagram, sendDatagram.length, address, port);
			
			System.out.println("SENT \t     | #" + currentCount + " | TO: " + address + "   | " + port + " | FROM: " + serverPort);
			
			// send with some probability loss
			if(random.nextFloat() >= lossProbability) {
				serverSocket.send(sendPacket);
			}
			
			while(true) {
				
				try {
					
					// receive packet
					byte[] receiveDatagram = new byte[DATAGRAM_SIZE];
					DatagramPacket receivePacket = new DatagramPacket(receiveDatagram, receiveDatagram.length);
					serverSocket.receive(receivePacket);
					Packet anotherPacket = new Packet(receiveDatagram);
					
					// if received acknowledgment packet type break the loop
					if(anotherPacket.getLength() == 12) {
						System.out.println("RECEIVED ACK | #" + currentCount + " | FROM: " + address + " | " + port + " | AT: " + serverPort);
						break;
					}
					
				} catch(SocketTimeoutException e) {
					
					// re-send the packet
					serverSocket.send(sendPacket);
					
					System.out.println("TIMEOUT!     | #" + currentCount + " | AT: " + address + "   | " + port + " | AT: " + serverPort);
					System.out.println("RESENT \t     | #" + currentCount + " | TO: " + address + "   | " + port + " | FROM: " + serverPort);
					
				}
				
			}
			
			 // increase sequence number
			currentCount++;

		}
		
		// send a terminating chunk
		Packet terminatePacket = new Packet(null, "terminate", 0);
		byte[] sendDatagram = terminatePacket.getPacketSegment();
		DatagramPacket sendPacket = new DatagramPacket(sendDatagram, sendDatagram.length , address, port);
		serverSocket.send(sendPacket);
		
		System.out.println("SUCCESSFULLY SENT FILE TO: " + address + " | " + port + " | FROM SERVER PORT " + serverPort + ". NUMBER OF PACKETS = " + packetCount + " & DATA SIZE = " + totalSize + " bytes.");
		
		// put the server port into the ready queue once again and close the socket
		Server.ports.add(serverPort);
		serverSocket.close();
		
	}
	
	public void run() {
		
		try {
			processPackets();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	
}

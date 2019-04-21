import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Handler implements Runnable {
	
	public static final int CHUNK_SIZE = 1000;
	public static final int DATAGRAM_SIZE = 1012;
	public static final int TIME_OUT = 800; // 3000 milliseconds
	
	public int totalSize;
	public int totalPackets;
	public int packetCount;

	private Thread thread;
	private DatagramSocket serverSocket;
	private int port;
	private InetAddress address;
	private DatagramPacket receivePacket;
	private byte[] receiveDatagram;
	private String filename;
	private int serverPort;
	private int seedValue;
	private int windowSize;
	private Random random;
	private double lossProbability;
	private Queue <Packet> packets; // Queue of packets
	int base; // base pointer
	int bufferPointer;
	int[] buffer;
	
	boolean fullWindow;
	int sentPointer = 0; // sequenceNumber of the next to be sent Packet
	
	final int ACK = 1;
	final int SENT = 2;
	
	LinkedList<PacketTimer> timers;
	ArrayList<Packet> packetList;
	LinkedList<Integer> receivedAkns;
	
	public Handler(int serverPort, String filename, double lossProbability, int seedValue, int clientPort, InetAddress clientAddress, int windowSize) throws SocketException {
		
		this.serverPort = serverPort;
		this.serverSocket = new DatagramSocket(serverPort);
		this.address = clientAddress;
		this.port = clientPort;
		this.filename = filename;
		this.lossProbability = lossProbability;
		this.seedValue = seedValue;
		this.windowSize = windowSize;
		
		receiveDatagram = new byte [DATAGRAM_SIZE];
		packets = new LinkedList<Packet>();
		receivedAkns = new LinkedList<Integer>();
		packetList = new ArrayList<Packet>();
		timers = new LinkedList<PacketTimer>();
		random = new Random((long) this.seedValue);
		
		fullWindow = false; // initially window is empty
		base = 0;
		
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
			packetList.add(sentPacket);
			sequenceNumber++;
		}
	
	}
	
	public void sendPacket(Packet nextPacket) throws IOException {
		
		// initialize packet
		byte[] sendDatagram = nextPacket.getPacketSegment();
		int sequenceNumber =  nextPacket.getSeqno();
		DatagramPacket sendPacket = new DatagramPacket(sendDatagram, sendDatagram.length, address, port);
		
		// send with some probability loss
		if(random.nextFloat() >= lossProbability) { // Send data
			serverSocket.send(sendPacket);
		}
		
		// set timers
		PacketTimer timer = new PacketTimer(sequenceNumber);
		PacketTimerTask action = new PacketTimerTask(sequenceNumber) {
			public void run() {
	        	int sequenceNumber = this.getSequenceNumber();
	        	if(!receivedAkns.contains(sequenceNumber)) {
	        		System.out.println("TIMEOUT!     | #" + sequenceNumber + " | WAIT: " + address + " | " + port + " | AT: " + serverPort);
	        		try {
	        			System.out.println("RESENT \t     | #" + sequenceNumber + " | TO " + address + "    | " + port + " | FROM: " + serverPort);
	        			Packet failedPacket = packetList.get(sequenceNumber);
        				sendPacket(failedPacket); // recursive call
        				this.cancel();
					} catch (IOException e) {
						e.printStackTrace();
					}
	        	}
	        }
		};
		timer.schedule(action, TIME_OUT);
		timers.addLast(timer);
		
	}
	
	public void deleteTimer(int number) {
		for(int i = 0; i < timers.size(); i++) {
			if(number == timers.get(i).getSequenceNumber()) {
				timers.remove(i).cancel();
			}
		}
	}
	
	public Packet receivePacket() throws IOException {
		
		// receive packet
		receivePacket = new DatagramPacket(receiveDatagram, receiveDatagram.length);
		serverSocket.receive(receivePacket);
		return new Packet(receiveDatagram);
		
	}
	
	public void start() {
		
		thread = new Thread(this);
		thread.start();
		
	}
	
	public void fillWindow() throws IOException {
		
		while(!packets.isEmpty() && sentPointer >= base && sentPointer < base + windowSize && sentPointer < packetCount) {
			
			Packet nextPacket = packets.remove();
			int sequenceNumber = nextPacket.getSeqno();
			sendPacket(nextPacket);
			buffer[sequenceNumber] = SENT;
			sentPointer++;
			
			System.out.println("SENT \t     | #" + sequenceNumber + " | TO: " + address + "   | " + port + " | FROM: " + serverPort);
			
		}
		
		fullWindow = true;
		
	}
	
	public boolean isFullWindow() {
		return fullWindow;
	}
	
	public void processPackets() throws IOException {
		
		System.out.println("SERVER PORT #" + serverPort + " HANDLES RECEIVED FILE REQUEST \"" + filename + "\" FROM: " + address + " | " + port + ".");
		
		// Read file
		Path filepath = Paths.get(filename);
		byte[] datapile = Files.readAllBytes(filepath);
		
		// assign overall size
		totalSize = datapile.length;
		
		// evaluate total number of packets
		packetCount = (int) Math.ceil((double)totalSize / CHUNK_SIZE);
		
		// initiate buffer with the overall packet size
		buffer = new int[packetCount];
		
		assignPackets(datapile);
		
		int orderPointer = 0; // sequenceNumber of the smallest non-yet acknowledged packet
		
		fillWindow();
		
		// reception
		while(true) {
			
			Packet anotherPacket =  receivePacket();
			int seqno = anotherPacket.getSeqno();
				
			if(anotherPacket.getLength() == 12) {
					
				System.out.println("RECEIVED ACK | #" + seqno + " | FROM: " + address + " | " + port + " | AT: " + serverPort);
					
				receivedAkns.add(seqno);
				deleteTimer(seqno);
				buffer[seqno] = ACK; // acknowledge packet
				
				// if first in-order packet is acknowledged, shift window
				while (orderPointer < packetCount && buffer[orderPointer] == ACK) { 
					base++;
					orderPointer++;
					fullWindow = false;
					fillWindow();
				}
				
				if(receivedAkns.size() == packetCount) // all packets are acknowledged
					break;
					
			}
			
		}
		
		// send a terminating chunk
		Packet terminatePacket = new Packet(null, "terminate", packetCount);
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

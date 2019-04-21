import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Server {
	
	public static final int DATAGRAM_SIZE = 1012;
	
	private int serverPort; // listening port
	private int windowSize; // unimportant in case of stop & wait
	private int seedValue;
	private double lossProbability;
	
	public static Queue <Integer> ports; // static & public to be modified, shared, and directly accessed by the Handler.
	private DatagramSocket serverSocket;
	
	public Server(String serverinfo) throws NumberFormatException, IOException {
		
		recordInfo(serverinfo);
		serverSocket = new DatagramSocket(serverPort);
		ports = new LinkedList<>(Arrays.asList(4000, 4001, 4002, 4003, 4004));
		
	}
	
	public void recordInfo(String serverinfo) throws NumberFormatException, IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(serverinfo));
		
		serverPort = Integer.parseInt(reader.readLine());
		windowSize = Integer.parseInt(reader.readLine());
		seedValue = Integer.parseInt(reader.readLine());
		lossProbability = Double.parseDouble(reader.readLine());
		
		reader.close();
		
	}

	public static void main(String[] args) throws IOException {
		
		Server server = new Server("serverinfo.txt");
		
		System.out.println("==================================================");
		System.out.println("====================== SERVER ====================");
		System.out.println("==================================================\r\n");
		
		// print initial client and server information
		System.out.println("SERVER ARGUMENTS:");
		System.out.println("-----------------");
		System.out.println("server_port ---> " + server.serverPort);
		System.out.println("loss_probability ---> " + server.lossProbability);
		System.out.println("seed_value ---> " + server.seedValue);	
		System.out.println("window_size ---> " + server.windowSize);
		
		System.out.println("\r\nWaiting for clients requests...");
		System.out.println("-------------------------------\r\n");
		
		while(true) {
		
			// retrieve client request
			byte[] receiveDatagram = new byte [DATAGRAM_SIZE];
			DatagramPacket receivePacket = new DatagramPacket(receiveDatagram, receiveDatagram.length);
			server.serverSocket.receive(receivePacket);
			
			// retrieve client data
			InetAddress clientAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			
			// send acknowledge packet to the client regarding the file transfer request
			Packet ackPacket = new Packet(null, "acknowledgement", 0);
			byte[] ackDatagram = ackPacket.getPacketSegment();
			DatagramPacket ackDatagramPacket = new DatagramPacket(ackDatagram, ackDatagram.length, clientAddress, clientPort);
			server.serverSocket.send(ackDatagramPacket);
			
			// retrieve requested file name
			Packet receivedPacket = new Packet(receiveDatagram);
			String filename =  new String(receivedPacket.getDataBytes());
			
			System.out.println("RECEIVED FILE REQUEST \"" + filename + "\" FROM CLIENT: " + clientAddress + " | " + clientPort);
			
			// create a handler thread
			if(!ports.isEmpty()) {
				int port = ports.remove();
				Handler handler = new Handler(port, filename, server.lossProbability, server.seedValue, clientPort, clientAddress, server.windowSize);
				handler.start();
			} else {
				System.out.println("CANNOT PROCEED WITH THE FILE REQUEST \"" + filename + "\". TRY AGAIN IN FEW MINUTES");
			}
			
		}
		
	}

}

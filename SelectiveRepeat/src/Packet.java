import java.nio.ByteBuffer;
import java.util.Arrays;

public class Packet {
		
	public static final int DATAGRAM_SIZE = 1012;
	public static final int CHUNK_SIZE = 1000;
	
	private byte[] packetData = new byte [CHUNK_SIZE];
	private byte[] packetSegment = new byte [DATAGRAM_SIZE];
	private int length;
	private int checksum;
	private int seqno;
	private byte[] dataBytes;
	boolean validity = true;
	
	public Packet(byte[] packetSegment) { // Analyze packet
		this.packetSegment = packetSegment;
		decode(packetSegment);
	}
	
	public Packet(byte[] packetData, String type, int seqno) { // Construct packet
		this.packetData = packetData;
		this.seqno = seqno;
		packetSegment = encode(packetData, type, seqno);
	}
	
	public void decode(byte[] udp_packet) {
		
		length = ByteBuffer.wrap(udp_packet, 0, 4).getInt();
		if(length == 0) // In case of termination packet
			return;
		checksum = ByteBuffer.wrap(udp_packet, 4, 4).getInt();
		seqno = ByteBuffer.wrap(udp_packet, 8, 4).getInt();
		dataBytes = new byte[length - 12];
		dataBytes = Arrays.copyOfRange(udp_packet, 12, length);
		if(checksum != evaluateChecksum(dataBytes)) {
			validity = false;
		} 
		
	}
	
	public byte[] encode(byte[] data, String type, int seqno) {
		
		int length = 12; // Initial Length of the packet that is of the header
		if(data != null) { // data is null in case of acknowledgment packet
			length = length + data.length;
		}
		ByteBuffer buffer = ByteBuffer.allocate(length);
		if(type == "terminate") {
			buffer.putInt(0);
		} else {
			buffer.putInt(length);
		}
		checksum = evaluateChecksum(data);
		buffer.putInt(checksum);
		buffer.putInt(seqno);
		if(data != null) {
			buffer.put(data);
		}
		
		// assign leftover parameters to the packet class
		this.length = length;
		
		return buffer.array();
		
	}
	
	public int evaluateChecksum(byte[] buffer) {
		
		// ~ ----> ones complement operator
		// << ---> left shift
		// & ----> AND ... it is used to promote bytes into unsigned bytes
		// promotions are only required whenever a bitwise operation is executed
		
		int length = 0;
		if(buffer != null) {
			length = buffer.length;
		}
		int sum = 0;
		int i = 0;
		
		// Handling pairs
		while(length > 1) {
			int data = ((buffer[i] << 8) & 0xFF00) | (buffer[i+1] & 0xFF);
			sum = sum + data;
			if((sum & 0xFFFF0000) > 0 ) { // 1's complement carry correction
				sum = sum & 0xFFFF;
				sum++;
			}
			i = i+2;
			length = length-2;
		}
		
		// Handling remaining byte in case of odd length buffers
		if(length > 0) {
			sum = sum + ((buffer[i] << 8) & 0xFF00);
			if((sum & 0xFFFF0000) > 0 ) { // 1's complement carry correction
				sum = sum & 0xFFFF;
				sum++;
			}
		}
		
		// Final 1's complement correction
		sum = ~sum;
		sum = sum & 0xFFFF;
		return (int)sum;
	}
	
	public boolean isValid() {
		return validity;
	}
	
	public byte[] getPacketData() {
		return packetData;
	}
	
	public byte[] getDataBytes() {
		return dataBytes;
	}
	
	public int getChecksum() {
		return checksum;
	}
	
	public int getLength() {
		return length;
	}
	
	public byte[] getPacketSegment() {
		return packetSegment;
	}
	
	public int getSeqno() {
		return seqno;
	}
	
}

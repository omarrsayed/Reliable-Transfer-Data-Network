import java.util.Timer;

public class PacketTimer extends Timer {

	private int sequenceNumber;
	
	public PacketTimer(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	
	public int getSequenceNumber() {
        return sequenceNumber;
    }
	
}

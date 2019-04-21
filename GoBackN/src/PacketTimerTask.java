import java.util.TimerTask;

class PacketTimerTask extends TimerTask {
		
    private int sequenceNumber;

    public PacketTimerTask(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

	public void run() {
		
	}
	
	public int getSequenceNumber() {
		return sequenceNumber;
	}

}
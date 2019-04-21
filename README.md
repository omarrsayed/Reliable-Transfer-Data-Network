# Reliable Data Transfer Network

This is almost equivalent to TCP protocol. It's all about adding reliable transfer layer on the top of UDP protocol. Such a layer provides
error recovery and guarnteed ordered delivery of packets.

The project involves three different algorithms for reliable data transfer: Stop&Wait, SelectiveRepeat and GoBackN implemented using JAVA.

# Important Definitions:

## Sockets:
Server and Clients are defined by sockets. They are unique. They are defined by IP address and port number. Packets
are sent through sockets.

## Server:
Server has one listening sockets and multiple handling sockets. These sockets all share the same IP address but different ports.
The handling sockets are used to handle multiple clients. The server can handle up to 4 clients at a time.

## Packet:
It's an array of 1012 bytes and structured as follows:
1. Header (12 bytes):
   - Length (4 bytes) : specifies the length of the packet.
   - Checksum (4 bytes) : A datum derived from data segment used for detecting errors.
   - Sequence Number (4 bytes) : It represents the order of the packet required for in-order transfer.
2. Data Segment (1000 bytes).

## Packet Types:
Data Packet : consists of header and data segment.
Acknowledgement Packet: consists of header only. It has a length of 12 bytes.
Terminating Packet: an empty packet. It has a length of zero.

## File Transfer Requests:
The client sends a packet to the server which contains the filename requested for transfer. Such an operation is backed by timeout
in case the datagram is lost. Unless the server sends an acknowledgement to the client, the client will keep resending the file transfer
request to the server.

## Packet Loss Simulation:
1. Loss Probability:
   - The server uses a random number generator with a given probability whether a datagram would be sent or ignored.
2. Packet Corruption:
   - Packet data segment is manipulated randomly at the client after reception. Checksum is used to identify such corruption and ignore it if so.

## When packets are resend?
Whenever a packet loss happens, the server will keep waiting for an acknowledgement of a packet that will never come from the cient
untill timeout occurs then the server resend the packet.

## Notes about Checksum:
1. There are different checksum algorithms. but we use Internet Checksum.
2. There is no checksum that can detect all errors. But our checksum has suffficient number of bits (16 bits) which makes undetected
errors very rare.

# Algorithms: 1. Stop & Wait

Stop & Wait actually implements the sliding window protocol but with a window size = 1.
Server sends only one packet at a time. It sends only the next packet after the previous packet is acknowledged.

# Algorithms: 2. Selective Repeat

1. Server:
   - If next packet is within the range of the base pointer and base pointer plus window size i.e. the window is not full, send the packet.
   - If a timeout is received for a packet, resend it.
   - If received an acknowledgement, mark the packet with that sequence number as received.
   - Shift the window to the smallest unacknowledged packet i.e. shift the base pointer.

2. Receiver:
   - If received packet is within the range of the window, send an acknowledgment packet with its sequence number. Otherwise, ignore the packet.
   - If received packet is out-of-order, buffer it.
   - If received packet is in-order, deliver all in-order packets and advance the window to the next not-yet received packet.

# Algorithms: 3. Go Back N

1. Server:
   - The same procedure as that of selective repeat except that if a timeout happens, all unacknowledged packets are resent.
2. Receiver:
   - If received packet is out-of-order, sends a cumulative acknowledgement that refers to sequence number of the last in-order received packet.

# How to run the program:

1. Specify the following argments in order, one item per line for the client information file:
   - IP address of the server.
   - Well-known port number of server.
   - Port number of client.
   - Filename to be requested for transfer.
   - Initial receiving sliding-window size in datagram units.

2. Specify the following argments in order, one item per line for the server information file:
   - Well-known port number of server.
   - Maximum sending sliding-window size in datagram units.
   - Random generator seed value.
   - Probability of datagram loss.

3. Run the Server.java Class, then run the ClientExecutioner.java Class.

4. For multiple clients, you can clone ClientExecutioner.java Class and run them.












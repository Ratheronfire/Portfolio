package java.project3;

import java.util.*;
import java.net.*;
import java.io.*;

public class DLLSender extends Thread
{
	private static final byte PREAMBLE = (byte) 0b01111110;
	private static final byte KIND = 0;

	private final int TIMEOUT = 1000;

	private DatagramSocket socket;
	private InetAddress serverAddress;
	private int serverPort;

	private File srcFile;
	private String logFilename;

	private byte[][] srcPackets;

	private boolean waitingForAck;

	int errType;

	Random rng;

	public DLLSender(File srcFile, String logFilename, String address, int port, int errType) throws SocketException, UnknownHostException
	{
		this.srcFile = srcFile;
		this.logFilename = logFilename;

		serverPort = port;

		serverAddress = InetAddress.getByName(address);

		socket = new DatagramSocket();
		socket.setSoTimeout(TIMEOUT);

		waitingForAck = false;

		this.errType = errType;

		rng = new Random();

		srcPackets = extractSourceBytes();
	}

	/**
	 * Reads the source file and produces a 2D array of bytes from it.
	 * Each subarray is 8 bytes long, ready to be added to a frame.
	 * Empty spaces are used to pad out the last array if needed.
	 * @return  An array of 8-byte arrays, read from the source file.
	 */
	public byte[][] extractSourceBytes()
	{
		int numOfPackets = (int) (srcFile.length() / 8) + 1;
		byte[][] sourceBytes = new byte[numOfPackets][8];

		try
		{
			BufferedReader srcReader = new BufferedReader(new FileReader(srcFile));

			for (int i = 0; i < numOfPackets; i++)
			{
				byte[] packet = new byte[8];

				for (int j = 0; j < 8; j++)
				{
					byte currentByte = (byte) srcReader.read();
					if (currentByte == -1)
						currentByte = (byte) ' ';

					packet[j] = currentByte;
				}

				sourceBytes[i] = packet;
			}
		}
		catch (IOException ioe)
		{
			System.out.println("Error reading source file.");
			ioe.printStackTrace();

			System.exit(3);
		}

		return sourceBytes;
	}

	/**
	 * Called when the thread is started.
	 */
	public void run()
	{
		try
		{
			sendFile();
		}
		catch (IOException ioe)
		{
			System.out.println("IOException caught in server thread.");
			ioe.printStackTrace();
		}
	}

	/**
	 * Sends a packet as a series of frames.
	 * Some packets may be corrupted or dropped 
	 * according to the user-set error level,
	 * to simulate either packet loss or a noisy signal.
	 */
	private void sendFile() throws IOException
	{
		boolean initReceived = false;
		boolean errorSimulated = false;
		byte[] initialFrame = buildFrame(new byte[] {(byte) srcPackets.length, 0, 0, 0, 0, 0, 0, 0}, (byte) 0);

		while (!initReceived)
		{
			if (!waitingForAck)
			{
				sendFrame(initialFrame);

				logEntry(initialFrame, "Sending initial packet to receiver.");
			}
			else
			{
				byte[] initAckRecv = receiveAck();

				logEntry(initAckRecv, "Initial ACK from receiver.");

				if (initAckRecv[3] > 0)
					initReceived = true;
			}
		}

		byte seq = 0;
		while (seq < srcPackets.length)
		{
			if (!waitingForAck)
			{
				byte[] frame = buildFrame(srcPackets[seq], seq);

				if (seq == 0 && errType == 2 && !errorSimulated)
				{
					System.out.println("SENDER: I'm corrupting this frame to simulate packet loss.");
					byte[] corruptedFrame = Arrays.copyOf(frame, frame.length);
					corruptedFrame[4] = (byte) rng.nextInt(256);

					logEntry(corruptedFrame, "Sending to receiver; this frame was corrupted.");
					errorSimulated = true;

					sendFrame(corruptedFrame);
				}

				if (seq == 1 && errType == 1 && !errorSimulated)
				{
					System.out.println("SENDER: I'm dropping this frame to simulate packet loss.");
					logEntry(frame, "Sending to receiver; this frame will be dropped.");
					errorSimulated = true;
				}
				else
				{
					logEntry(frame, "Sending to receiver.");
					sendFrame(frame);
				}
			}
			else
			{
				byte[] frameAck = receiveAck();

				if (frameAck[3] > 0)
				{
					logEntry(frameAck, "ACK from receiver.");
					seq++;
				}
				else
				{
					logEntry(frameAck, "NACK from receiver.");
					System.out.println("SENDER: Received NACK from receiver, resending packet.");
				}
			}
		}

		socket.close();
	}
	
	/**
	 * Builds a frame for a message, to be sent to the receiver.
	 * @param  message  The 8-byte message data.
	 * @param  seq      The sequence number of the packet.
	 */
	private byte[] buildFrame(byte[] message, byte seq)
	{
		byte[] frame = new byte[15];

		frame[0] = PREAMBLE;
		frame[1] = KIND;

		frame[2] = seq;
		frame[3] = (byte) 0;

		for (int i=0; i<8; i++)
			frame[4+i] = message[i];

		frame[12] = frame[13] = 0;
		frame[14] = PREAMBLE;

		frame = computeChecksum(frame);

		return frame;
	}

	/**
	 * Computes a 16-bit CRC checksum for a frame.
	 * @param  frame  The frame to compute a checksum for.
	 * 
	 * @return  The frame, with the checksum inserted at positions 12&13
	 */
	public byte[] computeChecksum(byte[] frame)
	{
		byte[] checksum = CRC16.go(Arrays.copyOfRange(frame, 0, 12));

		frame[12] = checksum[0];
		frame[13] = checksum[1];

		return frame;

	}

	/**
	 * Sends a frame to the receiver.
	 * @param  frame  The frame to send.
	 */
	private void sendFrame(byte[] frame) throws IOException
	{
		DatagramPacket sendPacket = new DatagramPacket(frame, frame.length, serverAddress, serverPort);

		try
		{
			socket.send(sendPacket);

			waitingForAck = true;
		}
		catch (SocketTimeoutException ste)
		{
			System.out.println("Sender timed out sending to server.");
		}
	}

	/**
	 * Waits for an ACK from the server.
	 * @return  The server's response packet.
	 */
	private byte[] receiveAck() throws IOException
	{
		byte[] receiveData = new byte[15];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		try
		{
			socket.receive(receivePacket);

			waitingForAck = false;
		}
		catch (SocketTimeoutException ste)
		{
			System.out.println("Sender timed out waiting for ACK.");
		}

		return receiveData;
	}

	/**
	 * Extracts data from a frame, and wites it to the source log file.
	 * @param  frame      The frame to log data from.
	 * @param  comment    A message explaining the source of the data.
	 */
	private void logEntry(byte[] frame, String comment)
	{
		char[] dataChars = new char[8];

		for (int i=4; i<12; i++)
			dataChars[i-4] = (char) frame[i];

		int checksumVal = frame[12] * 256 + frame[13];

		DLLSim.writeFormatted(logFilename, (int) frame[2], dataChars, checksumVal, comment);
	}
}

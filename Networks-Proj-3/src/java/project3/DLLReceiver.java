package java.project3;

import java.util.*;
import java.net.*;
import java.io.*;

public class DLLReceiver extends Thread
{

	private final int TIMEOUT = 1000;

	private DatagramSocket socket;
	private InetAddress clientAddress;
	private int clientPort;

	private String outFilename;
	private String destLogFilename;
	private String recvLogFilename;

	private byte[] receiveData;

	public DLLReceiver(String outFilename, String destLogFilename, String recvLogFilename, String address, int port) throws SocketException, UnknownHostException
	{
		this.outFilename = outFilename;
		this.destLogFilename = destLogFilename;
		this.recvLogFilename = recvLogFilename;

		clientAddress = InetAddress.getByName(address);
		clientPort = port;

		socket = new DatagramSocket(clientPort);
		socket.setSoTimeout(TIMEOUT);

		receiveData = new byte[15];
	}

	/**
	 * Called when the thread is started.
	 */
	public void run()
	{
		try
		{
			receiveFile();
		}
		catch (IOException ioe)
		{
			System.out.println("IOException caught in receiver thread.");
			ioe.printStackTrace();
		}
	}

	/**
	 * Receives a file as a series of packets.
	 * If a packet has already been seen, or has been corrupted,
	 * it will be dropped.
	 */
	private void receiveFile() throws IOException
	{
		boolean initialReceived = false;

		int seq = 0;
		int framesToReceive = 0;

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		while (!initialReceived)
		{
			socket.receive(receivePacket);

			logEntry(receiveData, "Initial packet.", recvLogFilename);

			boolean ack = true;

			if (!isValid(receiveData))
				ack = false;
			else
			{
				framesToReceive = (int) receiveData[4];
				initialReceived = true;
			}

			clientAddress = receivePacket.getAddress();
			clientPort = receivePacket.getPort();

			acknowledgePacket(receiveData, ack);
			logEntry(receiveData, "ACKing initial packet.", recvLogFilename);
		}

		byte[] clientData = new byte[8 * framesToReceive];

		while (seq < framesToReceive)
		{
			try
			{
				socket.receive(receivePacket);
				boolean ack = true;

				if (!isValid(receiveData))
				{
					byte[] computedChecksum = CRC16.go(Arrays.copyOfRange(receiveData, 0, 14));

					receiveData[12] = computedChecksum[0];
					receiveData[13] = computedChecksum[1];

					logEntry(receiveData, "Received corrupt packet from sender.", recvLogFilename);

					System.out.println("RECEIVER: This packet was corrupted, NACKing.");

					ack = false;
				}
				else
				{
					if (receiveData[2] < seq)
					{
						logEntry(receiveData, "Packet already ACKed, dropping.", recvLogFilename);
					}
					else
					{
						logEntry(receiveData, "Received packet from sender.", recvLogFilename);
						logEntry(receiveData, "Packet ACKed.", destLogFilename);

						for (int i=0; i<8; i++)
							clientData[seq*8 + i] = receiveData[i+4];
						
						seq++;
					}
				}

				clientAddress = receivePacket.getAddress();
				clientPort = receivePacket.getPort();

				acknowledgePacket(receiveData, ack);
			}
			catch (SocketTimeoutException ste)
			{
				System.out.println("Receiver timed out.");
			}
		}

		socket.close();

		FileOutputStream destOut = new FileOutputStream(outFilename);
		destOut.write(clientData);
		destOut.close();
	}

	/**
	 * Send an ACK packet back to the sender.
	 * If the packet was corrupted, send a 
	 * NACK instead.
	 */
	private void acknowledgePacket(byte[] frame, boolean ack) throws IOException
	{
		if (ack)
			frame[3] = 1;
		else
			frame[3] = -1;

		DatagramPacket ackPacket = new DatagramPacket(frame, frame.length, clientAddress, clientPort);
		socket.send(ackPacket);
	}

	/**
	 * Verifies the 16-bit CRC checksum in a received frame.
	 * @param  frame  The frame to check.
	 * 
	 * @return  True if the computed checksum matches the provided checksum, else false.
	 */
	private boolean isValid(byte[] frame)
	{
		return CRC16.valid(Arrays.copyOfRange(frame, 0, 14));
	}

	/**
	 * Extracts data from a frame, and wites it to a log file.
	 * @param  frame      The frame to log data from.
	 * @param  comment    A message explaining the source of the data.
	 * @param  logTarget  The log file to write to.
	 *                    DestLog is used for acknowledged packets.
	 *                    RecvLog is used for all received packets.
	 */
	private void logEntry(byte[] frame, String comment, String logTarget)
	{
		char[] dataChars = new char[8];

		for (int i=4; i<12; i++)
			dataChars[i-4] = (char) frame[i];

		int checksumVal = frame[12] * 256 + frame[13];

		DLLSim.writeFormatted(logTarget, (int)frame[2], dataChars, checksumVal, comment);
	}
}

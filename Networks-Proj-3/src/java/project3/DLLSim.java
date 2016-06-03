package java.project3;

import java.io.*;
import java.net.*;
import java.lang.*;

public class DLLSim
{
	private static File srcFile;
	private static String outFilename;

	private static final String ADDRESS = "localhost";
	private static final int PORT = 2345;

	private static final String SOURCE_LOG = "srcLogFile.txt";
	private static final String DEST_LOG = "destLogFile.txt";
	private static final String RECV_LOG = "recvLogFile.txt";

	private static final String FORMAT_HEADER = "%-8s%-10s%-10s%-20s";
	private static final String FORMAT_DATA = "%-8d%-10s%-10d%-20s";

	public static void main(String[] args)
	{
		if (args.length != 3)
			printUsageMsg();

		int errType = Integer.parseInt(args[0]);

		srcFile = new File(args[1]);
		outFilename = args[2];

		if (errType < 0 || errType > 2 || !srcFile.exists())
			printUsageMsg();

		initLogs();

		try
		{
			DLLReceiver receiver = new DLLReceiver(outFilename, DEST_LOG, RECV_LOG, ADDRESS, PORT);
			receiver.start();

			DLLSender sender = new DLLSender(srcFile, SOURCE_LOG, ADDRESS, PORT, errType);
			sender.start();

			receiver.join();
			sender.join();

			System.out.println("The simulation is over, check the log/output files for the results!");
		}
		catch (Exception e)
		{
			System.out.println("Encountered an error running the simulation.");
			e.printStackTrace();

			System.exit(2);
		}
	}

	/**
	 * Writes headers to the log files, overwriting them if they exist.
	 */
	private static void initLogs()
	{
		writeToFile(SOURCE_LOG, String.format(FORMAT_HEADER, "Seq #", "Data", "Checksum", "Comments"), false);
		writeToFile(DEST_LOG, String.format(FORMAT_HEADER, "Seq #", "Data", "Checksum", "Comments"), false);
		writeToFile(RECV_LOG, String.format(FORMAT_HEADER, "Seq #", "Data", "Checksum", "Comments"), false);
	}

	/**
	 * Prints a message explaining how to use the program, then exits.
	 */
	public static void printUsageMsg()
	{
		System.out.println("USAGE:\nDLLSim errType srcFile destFile\n" +
				   "	errType must be 0 (no errors), 1 (lossy), or 2 (noisy)\n" +
				   "	srcFile must be an existing file.\n	destFile will be overwritten if existing.");

		System.exit(1);
	}

	/**
	 * Writes data from a received/sent packet to a log file, and formats it into columns.
	 * @param  filename  The path of the file to write to.
	 * @param  seqNum    The sequence number of the packet.
	 * @param  data      The 8 bytes of data from the packet.
	 * @param  checksum  The 16-bit checksum calculated for the packet.
	 * @param  comments  A message explaining the source/destination of the packet.
	 */
	public static void writeFormatted(String filename, int seqNum, char[] data, int checksum, String comments)
	{
		writeToFile(filename, String.format(FORMAT_DATA, seqNum, new String(data), checksum, comments), true);
	}

	/**
	 * Writes text to a log file.
	 * @param  filename  The path of the file to write to.
	 * @param  text      The text to write.
	 * @param  append    If true, the text will be added onto the file,
	 *                   otherwise the file will be overwritten.
	 */
	public static void writeToFile(String filename, String text, boolean append)
	{
		try
		{
			FileWriter fw = new FileWriter(filename, append);
			fw.write(text + "\n");
			fw.close();
		}
		catch (IOException ioe)
		{
			System.out.println("Error writing to " + filename);
			ioe.printStackTrace();
		}
	}
}

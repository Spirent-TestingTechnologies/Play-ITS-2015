/*
 * Sources: 
 * https://github.com/dmatej/java-cardiag
 * https://github.com/pires/obd-java-api
 * http://stackoverflow.com/questions/18431424/unable-to-communicate-with-elm327-bluetooth
 */

package canfilter;

import javax.sound.sampled.Port;

import org.apache.commons.lang3.StringUtils;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import java.io.*;
import java.util.Vector;


public class ELM327 {
	private int RS232 = 0;
	private int BLUETOOTH = 1;
	private int usedConnection;
	private static final String RESPONSE_OK = "OK";
	private static final String RESPONSE_TERMINALCHAR = ">";

	// Bluetooth Variables
	private static Object lock = new Object();
	private static Vector remdevices = new Vector();
	private static String connectionURL = null;

	// Serial Variables:
	Socket sock = null;
	Hashtable<String, String> commandHashTable = new Hashtable<String, String>();

	private String portName; // //TODO: never set?
	private Long commandTimeout = 3000l; //

	private final SerialPort serialPort = new SerialPort(portName);


	public void main(String[] args) {
		initConnection(args[0]);
		initCommandHashTable();

	}

	private void initCommandHashTable() {
		// http://www.totalcardiagnostics.com/support/Knowledgebase/Article/View/21/0/genericmanufacturer-obd2-codes-and-their-meanings
		// TODO Auto-generated method stub
		String manufacturer = executeCommand("09");

	}

	public void initConnection(String type) {
		if (type == "rs232") {
			usedConnection = RS232;
			try {
				// Page 7 Manual elm327.pdf
				serialPort.setParams(38400, 8, 1, 0, true, true);
			} catch (SerialPortException e) {
				e.printStackTrace();
			}
		} else if (type == "bluetooth") {
			sock = new Socket();
			// TODO implement Bluetooth version
		} else {
			System.out.println("please choose rs232 or bluetooth");
		}

	}

	/**
	 * Reads responses line by line from the buffer until
	 * {@value #RESPONSE_TERMINALCHAR} comes or timeout occurs.
	 *
	 * @return a list of lines in response, never null and never empty list.
	 * @throws PortCommunicationException
	 *             - if there was no response or prompt was missing.
	 */
	public List<String> readResponse() {

		try {
			final StringBuilder buffer = new StringBuilder(256);
			final long start = System.currentTimeMillis();
			while (true) {
				if (start + commandTimeout < System.currentTimeMillis()) {
					System.out.println("Timeout %dms occured.");
				}

				if (usedConnection == RS232) {
					if (serialPort.getInputBufferBytesCount() == 0) {
						Thread.yield();
						continue;
					}
				} else if (usedConnection == BLUETOOTH) {
					// TODO
				}

				String string = "";
				if (usedConnection == RS232) {
					string = serialPort.readString();
				} else if (usedConnection == BLUETOOTH) {
					// TODO
					string = "";
				}

				buffer.append(string).append('\r');
				if (StringUtils.endsWith(string, RESPONSE_TERMINALCHAR)) {
					buffer.setLength(buffer.length() - 2);
					break;
				}
			}
			final String response = buffer.toString();
			if (response == null || response.trim().isEmpty()) {
				System.out.println("Retrieved no response from the port.");
			}
			final String[] lines = StringUtils.split(response, '\r');
			final List<String> responses = new ArrayList<String>(lines.length);
			for (String line : lines) {
				final String trimmed = StringUtils.trimToNull(line);
				if (trimmed != null) {
					responses.add(trimmed);
				}
			}

			return responses;
		} catch (SerialPortException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes command and arguments to the port.
	 *
	 * @param command
	 *            - command and it's arguments.
	 * @throws PortCommunicationException
	 */
	public void writeln(String... command) {
		if (usedConnection == RS232) {
			try {
				for (String commandPart : command) {
					serialPort.writeString(commandPart);
				}
				serialPort.writeString("\r\n");
			} catch (SerialPortException e) {
				throw new RuntimeException(e);
			}
		} else if (usedConnection == BLUETOOTH) {
			// TODO
		}
	}

	/**
	 * @param value
	 * @return 1 for true, 0 for false.
	 */
	protected final String translate(final boolean value) {
		return value ? "1" : "0";
	}

	/**
	 * Checks for OK in port response.
	 *
	 */
	protected void checkOkResponse() {
		final List<String> response = readResponse();
		if (response == null || response.isEmpty()) {
			throw new RuntimeException("No response.");
		}
		final int resultCodeIndex = response.size() - 1;
		if (!RESPONSE_OK.equalsIgnoreCase(response.get(resultCodeIndex))) {
			throw new RuntimeException("Command unsuccessful! Response: "
					+ response);
		}
	}

	/**
	 * Executes an AT command and returns an answer.
	 *
	 * @param command
	 *            - an AT command to execute.
	 * @return an answer of the command.
	 */
	public String executeCommand(final String command) {
		writeln("AT", command);
		return readResponse().get(0);
	}

	/**
	 * Sends ATE signal and sets the command echo on/off
	 *
	 * @param on
	 */
	public void setEcho(final boolean on) {
		writeln("ATE", translate(on));
		checkOkResponse();
	}

	/**
	 * Sends ATL signal and sets the line termination on/off
	 *
	 * @param on
	 */
	public void setLineTermination(final boolean on) {
		writeln("ATL", translate(on));
		checkOkResponse();
	}

	/**
	 * Sends AT Z command, resets the communication.
	 *
	 */
	public void reset() {
		final String response = executeCommand("Z");
		// with echo on the first line will be ATZ (sent command)
		// another line will be a device type identification.
		if (response == null) {
			throw new RuntimeException("Command unsuccessful! Response: "
					+ response);
		}
	}

	/**
	 * Closes the port.
	 */
	// @Override
	public void close() {
		if (usedConnection == RS232) {
			try {
				serialPort.closePort();
			} catch (SerialPortException e) {
				throw new IllegalStateException("Cannot close the port.", e);
			}
		} else if (usedConnection == BLUETOOTH) {

		}

	}

}

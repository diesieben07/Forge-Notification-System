package de.take_weiland.forgenotif.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.PublicKey;
import java.util.logging.Logger;

import de.take_weiland.forgenotif.network.FNSPacket.ProtocolException;

public class ListenerThread extends Thread {

	private final ServerSocket inSocket;
	private final Logger logger;
	private final PublicKey key;
	
	private volatile boolean shouldListen;
	
	public ListenerThread(int port, InetAddress adress, PublicKey key, Logger logger) throws IOException {
		inSocket = new ServerSocket(port, 0, adress);
		this.logger = logger;
		this.key = key;
		shouldListen = true;
	}
	
	public void shutdown() {
		shouldListen = false;
		try {
			inSocket.close();
		} catch (IOException e) { }
	}
	
	@Override
	public void run() {
		while (shouldListen) {
			try {
				Socket socket = inSocket.accept();
				logger.fine("Starting TCP Thread for " + socket.getInetAddress().getHostAddress());
				new TCPThread(socket, key, logger).start();
			} catch (IOException e) {
				logger.info("Exception during network handling. This does not need to be a bad thing.");
				e.printStackTrace();
			}
		}
		try {
			inSocket.close();
		} catch (IOException e) { }
		logger.info("Stopping listener thread...");
	}

	@Override
	public synchronized void start() {
		logger.info("Starting Listener Thread on " + inSocket.getInetAddress().toString() + ":" + inSocket.getLocalPort());
		super.start();
	}

}
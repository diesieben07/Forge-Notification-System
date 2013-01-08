package de.take_weiland.forgenotif.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import de.take_weiland.forgenotif.core.ForgeNotifModContainer;
import de.take_weiland.forgenotif.network.FNSPacket.ProtocolException;

public class TCPThread extends Thread {

	private final Socket socket;
	private final Logger logger;
	private boolean keepAlive = true;
	
	public TCPThread(Socket socket, Logger logger) {
		this.socket = socket;
		this.logger = logger;
	}
	
	@Override
	public void run() {
		InputStream inStream = null;
		OutputStream outStream = null;
		try {
			socket.setSoTimeout(200);
			inStream = socket.getInputStream();
			outStream = socket.getOutputStream();
			
			while (keepAlive) {
				FNSPacket packet = null;
				try {
					packet = FNSPacket.parsePacket(new DataInputStream(inStream));
				} catch (ProtocolException e) {
					logger.info("Protocol exception from client " + socket.getInetAddress().getHostAddress());
					e.printStackTrace();
					inStream.close();
					outStream.close();
					return;
				}
				if (packet.handleInMinecraftContext()) {
					ForgeNotifModContainer.instance().getTickHandler().schedulePacket(packet);
				} else {
					packet.handle(this);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				inStream.close();
				outStream.close();
			} catch (Exception e2) { }
		}
	}
	
	protected void closeConnection() {
		keepAlive = false;
	}
}
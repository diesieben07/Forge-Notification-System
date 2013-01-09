package de.take_weiland.forgenotif.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import de.take_weiland.forgenotif.core.ForgeNotifModContainer;
import de.take_weiland.forgenotif.network.FNSPacket.ProtocolException;

public class TCPThread extends Thread {

	private final Socket socket;
	private final Logger logger;
	private final PublicKey key;
	private Cipher cipher;
	private MessageDigest sha1;
	private boolean keepAlive = true;
	
	public TCPThread(Socket socket, PublicKey key, Logger logger) {
		this.socket = socket;
		this.logger = logger;
		this.key = key;
		try {
			this.cipher = Cipher.getInstance("RSA");
		} catch (Exception e) {
			this.cipher = null;
		}
		try {
			this.sha1 = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			this.sha1 = null;
		}
	}
	
	@Override
	public void run() {
		InputStream inStream = null;
		OutputStream outStream = null;
		try {
			socket.setSoTimeout(60000);
			inStream = socket.getInputStream();
			outStream = socket.getOutputStream();
			
			if (cipher == null || this.sha1 == null) {
				socket.close();
				return;
			}
			
			while (keepAlive) {
				FNSPacket packet = null;
				try {
					packet = FNSPacket.parsePacket(new DataInputStream(inStream), this, cipher, key, sha1);
					packet.handle();
				} catch (ProtocolException e) {
					logger.info("Protocol exception from client " + socket.getInetAddress().getHostAddress());
					e.printStackTrace();
					inStream.close();
					outStream.close();
					return;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				socket.close();
			} catch (Exception e2) { }
		}
	}
	
	protected void closeConnection() {
		keepAlive = false;
	}
}
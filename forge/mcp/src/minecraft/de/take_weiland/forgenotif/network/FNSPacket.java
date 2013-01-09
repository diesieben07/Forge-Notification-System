package de.take_weiland.forgenotif.network;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;


public abstract class FNSPacket {
	
	private static final int PACKET_COUNT = 1;
	private static final String PROTOCOL_VERSION = "1.0";
	
	private static Map<String, Class<? extends FNSPacket>> nameToClassMap = new HashMap<String, Class<? extends FNSPacket>>(PACKET_COUNT);
	private static Map<Class<? extends FNSPacket>, String> classToNameMap = new HashMap<Class<? extends FNSPacket>, String>(PACKET_COUNT);
	
	protected static Collection<PacketHandler> registeredPacketHandlers = new HashSet<PacketHandler>();
	
	private static void addMapping(String name, Class<? extends FNSPacket> clazz) {
		nameToClassMap.put(name, clazz);
		classToNameMap.put(clazz, name);
	}
	
	static {
		addMapping("REWARD", PacketReward.class);
	}
	
	public static void registerPacketHandler(PacketHandler handler) {
		registeredPacketHandlers.add(handler);
	}
	
	public static FNSPacket parsePacket(DataInput input, TCPThread tcpThread, Cipher cipher, PublicKey key, MessageDigest sha1) throws IOException, ProtocolException {
		try {
			int packetLength = input.readShort();
			
			if (packetLength <= 20) {
				throw new ProtocolException("Illegal Packet Size!");
			}
			
			byte[] cryptHash = new byte[128];
			input.readFully(cryptHash);
			
			byte[] contents = new byte[packetLength - 128];
			input.readFully(contents);
			
			byte[] decryptHash = null;
			
			try {
				cipher.init(Cipher.DECRYPT_MODE, key);
				decryptHash = cipher.doFinal(cryptHash);
			} catch (InvalidKeyException e) {
				throw new IOException("Decrypt error.");
			} catch (IllegalBlockSizeException e) {
				throw new ProtocolException("Invalid Crypt Hash!");
			} catch (BadPaddingException e) {
				throw new ProtocolException("Invalid Crypt Hash!");
			}
			
			if (decryptHash == null) {
				throw new ProtocolException("Invalid Crypt Hash!");
			}
			
			byte[] contentHash = sha1.digest(contents);
			
			if (!Arrays.equals(decryptHash, contentHash)) {
				throw new ProtocolException("Hash doesn't match!");
			}
			
			ByteArrayDataInput in = ByteStreams.newDataInput(contents);
			
			String versionReceived = readString(in);
			if (!versionReceived.equals(PROTOCOL_VERSION)) {
				throw new ProtocolException("Unmatching Packet Versions!");
			}
			
			FNSPacket packet = constructByName(readString(in));
			
			if (packet == null) {
				throw new ProtocolException("Illegal Packet Type!");
			}
			
			packet.readData(in);
			
			if (!in.readBoolean()) {
				tcpThread.closeConnection();
			}
			return packet;
		} catch (EOFException e) {
			e.printStackTrace();
			throw new ProtocolException("Packet Structure corrupted!");
		} catch (IllegalStateException e) {
			e.printStackTrace();
			throw new ProtocolException("Packet Structure corrupted!");
		}
	}
	
	public static FNSPacket constructByName(final String name) {
		final Class<? extends FNSPacket> clazz = nameToClassMap.get(name.toUpperCase());
		if (clazz == null) {
			return null;
		} else {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				return null;
			}
		}
	}
	
	protected static String readString(DataInput input) throws IOException {
		int stringLength = input.readShort();
		byte[] stringBytes = new byte[stringLength];
		input.readFully(stringBytes);
		try {
			return new String(stringBytes, "UTF8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}
	
	protected static void writeString(ByteArrayDataOutput out, String string) {
		if (string.length() > Short.MAX_VALUE) {
			throw new IllegalArgumentException("String longer than maximum allowed!");
		}
		out.writeShort(string.length());
		try {
			out.write(string.getBytes("UTF8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("UTF8 not supported!", e);
		}
	}
	
	public static class ProtocolException extends Exception {

		public ProtocolException() { }

		public ProtocolException(String message) {
			super(message);
		}
	}
	
	public final void handle() {
		for (PacketHandler handler : registeredPacketHandlers) {
			handle(handler);
		}
	}
	
	protected abstract void handle(PacketHandler handler);
	
	protected abstract void readData(DataInput in) throws ProtocolException, IOException;
	
	protected abstract void writeData(ByteArrayDataOutput out);
}
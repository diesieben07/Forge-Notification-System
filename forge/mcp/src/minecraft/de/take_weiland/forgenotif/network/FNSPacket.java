package de.take_weiland.forgenotif.network;

import java.io.DataInput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.MinecraftServer;

import com.google.common.io.ByteArrayDataOutput;

import de.take_weiland.forgenotif.PacketTickHandler;

public abstract class FNSPacket {
	
	private static final int DEFAULT_MAP_SIZE = 1;
	
	private static Map<String, Class<? extends FNSPacket>> nameToClassMap = new HashMap<String, Class<? extends FNSPacket>>(1);
	private static Map<Class<? extends FNSPacket>, String> classToNameMap = new HashMap<Class<? extends FNSPacket>, String>(1);
	
	private static void addMapping(String name, Class<? extends FNSPacket> clazz) {
		nameToClassMap.put(name, clazz);
		classToNameMap.put(clazz, name);
	}
	
	static {
		addMapping("REWARD", PacketReward.class);
		addMapping("CLOSE", PacketCloseConnection.class);
	}
	
	public static FNSPacket parsePacket(DataInput in) throws IOException, ProtocolException {
		try {
			byte[] hash = new byte[20];
			in.readFully(hash);
			for (int i = 0; i < 3; i++) {
				readNullbyte(in);
			}
			FNSPacket packet = constructByName(readString(in));
			
			if (packet == null) {
				throw new ProtocolException("Illegal Packet Type!");
			}
			
			readNullbyte(in);
			String versionReceived = readString(in);
			if (!versionReceived.equals(packet.getVersion())) {
				throw new ProtocolException("Unmatching Packet Versions!");
			}
			
			readNullbyte(in);
			
			packet.readData(in);
			
			readNullbyte(in);
			return packet;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			throw new ProtocolException("Packet Structure corrupted!");
		}
		
	}
	
	public static FNSPacket constructByName(final String name) {
		final Class<? extends FNSPacket> clazz = nameToClassMap.get(name);
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
	
	protected static void readNullbyte(DataInput in) throws ProtocolException, IOException {
		if (in.readByte() != 0) {
			throw new ProtocolException("Expected Nullbyte!");
		}
	}
	
	public static class ProtocolException extends Exception {

		public ProtocolException() { }

		public ProtocolException(String message) {
			super(message);
		}
	}
	
	protected abstract void readData(DataInput in) throws ProtocolException, IOException;
	
	protected abstract void writeData(ByteArrayDataOutput out);
	
	protected abstract String getVersion();
	
	public void handle(TCPThread connectionThread) { }
	
	public void handle(MinecraftServer server) { }
	
	/**
	 * determines if this packet needs to be handeled in the main Minecraft Thread (e.g. when it performs Game influencing activitiy)<br>
	 * if this is true {@link #handle(MinecraftServer server)} is called the next time the {@link PacketTickHandler} ticks (once per second), otherwise {@link #handle(TCPThread)} is called from within the TCPConnection Thread
	 * @return true if it needs to be scheduled for the next TickHandler tick
	 */
	protected boolean handleInMinecraftContext() {
		return false;
	}
}
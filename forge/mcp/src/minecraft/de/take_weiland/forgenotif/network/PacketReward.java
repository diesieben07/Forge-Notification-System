package de.take_weiland.forgenotif.network;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.server.MinecraftServer;

import com.google.common.io.ByteArrayDataOutput;

public class PacketReward extends FNSPacket {

	private String username;
	private long timestamp;
	
	@Override
	protected String getVersion() {
		return "1.0";
	}

	@Override
	protected void readData(DataInput in) throws ProtocolException, IOException {
		username = readString(in);
		readNullbyte(in);
		timestamp = in.readLong();
	}

	@Override
	protected void writeData(ByteArrayDataOutput out) {
		writeString(out, username);
		out.writeByte(0);
		out.writeLong(timestamp);
	}

	@Override
	public void handle(MinecraftServer server) {
		System.out.println("Handling: " + username + " " + timestamp);
	}

	@Override
	protected boolean handleInMinecraftContext() {
		return true;
	}
}
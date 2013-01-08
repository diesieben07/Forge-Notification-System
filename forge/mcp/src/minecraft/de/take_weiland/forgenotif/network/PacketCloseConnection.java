package de.take_weiland.forgenotif.network;

import java.io.DataInput;
import java.io.IOException;

import com.google.common.io.ByteArrayDataOutput;

public class PacketCloseConnection extends FNSPacket {

	@Override
	protected String getVersion() {
		return "1.0";
	}
	
	@Override
	protected void readData(DataInput in) throws ProtocolException, IOException {
		readNullbyte(in);
	}

	@Override
	protected void writeData(ByteArrayDataOutput out) {
		out.writeByte(0);
	}

	@Override
	public void handle(TCPThread connectionThread) {
		connectionThread.closeConnection();
	}
}
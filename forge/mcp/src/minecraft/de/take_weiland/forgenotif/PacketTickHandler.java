package de.take_weiland.forgenotif;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import de.take_weiland.forgenotif.network.FNSPacket;

public class PacketTickHandler implements IScheduledTickHandler {

	private List<FNSPacket> pendingPackets = new ArrayList<FNSPacket>();
	
	private final MinecraftServer server;
	
	public PacketTickHandler(MinecraftServer server) {
		this.server = server;
	}
	
	public void schedulePacket(FNSPacket packet) {
		synchronized (pendingPackets) {
			pendingPackets.add(packet);
		}
	}
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		synchronized (pendingPackets) {
			Iterator<FNSPacket> it = pendingPackets.iterator();
			while (it.hasNext()) {
				it.next().handle(server);
				it.remove();
			}
		}
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) { }

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.SERVER);
	}

	@Override
	public String getLabel() {
		return "FNSPacketHandling";
	}

	@Override
	public int nextTickSpacing() {
		return 20; // every second should be enough
	}

}

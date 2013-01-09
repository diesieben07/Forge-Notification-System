package de.take_weiland.forgenotif;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import de.take_weiland.forgenotif.core.ForgeNotifModContainer;
import de.take_weiland.forgenotif.network.FNSPacket;
import de.take_weiland.forgenotif.network.PacketHandler;
import de.take_weiland.forgenotif.network.PacketReward;

public class PacketTickHandler implements IScheduledTickHandler, PacketHandler {

	private List<Reward> pendingRewards = new ArrayList<Reward>();
	
	private final MinecraftServer server;
	private final Logger logger;
	private boolean continueProcessing = true;
	private boolean inTick = false;
	
	public PacketTickHandler(MinecraftServer server, Logger logger) {
		this.server = server;
		this.logger = logger;
		FNSPacket.registerPacketHandler(this);
	}
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		if (!continueProcessing) {
			return;
		}
		inTick = true;
		synchronized (pendingRewards) {
			Iterator<Reward> it = pendingRewards.iterator();
			while (it.hasNext()) {
				Reward reward = it.next();
				EntityPlayer player = server.getConfigurationManager().getPlayerForUsername(reward.getUsername());
				boolean rewardGiven = false;
				if (server.isServerRunning() && player != null) {
					int firstEmpty = player.inventory.getFirstEmptyStack();
					if (firstEmpty != -1) {
						player.inventory.setInventorySlotContents(firstEmpty, new ItemStack(Item.ingotGold));
						rewardGiven = true;
					}
				}
				if (rewardGiven) {
					it.remove();
				}
			}
		}
		inTick = false;
	}
	
	public boolean isProcessing() {
		return inTick;
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) { }

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.SERVER);
	}

	@Override
	public String getLabel() {
		return "FNSRewardHandling";
	}

	@Override
	public int nextTickSpacing() {
		return 1200; // every minute should be enough
	}

	@Override
	public void handleReward(PacketReward packet) {
		synchronized (pendingRewards) {
			pendingRewards.add(new Reward(packet));
		}
	}
	
	public void stopProcessing() {
		logger.info("Stopping Reward TickHandler...");
		continueProcessing = false;
	}
	
	public void writeRewards(NBTTagCompound nbt) {
		NBTTagList rewardList = new NBTTagList();
		synchronized (pendingRewards) {
			for (Reward reward : pendingRewards) {
				rewardList.appendTag(reward.writeToNbt());
			}
		}
		System.out.println("Saved " + rewardList.tagCount() + " rewards to disk");
		nbt.setTag("rewards", rewardList);
	}
	
	public void readRewards(NBTTagCompound nbt) {
		NBTTagList rewardList = nbt.getTagList("rewards");
		synchronized (pendingRewards) {
			for (int i = 0; i < rewardList.tagCount(); i++) {
				pendingRewards.add(new Reward((NBTTagCompound)rewardList.tagAt(i)));
			}
		}
		System.out.println("loaded " + rewardList.tagCount() + " rewards from disk");
	}
}
package de.take_weiland.forgenotif;

import net.minecraft.nbt.NBTTagCompound;
import de.take_weiland.forgenotif.network.PacketReward;

public class Reward {
	private final String username;
	private final long timestamp;
	
	public Reward(String username, long timestamp) {
		this.username = username;
		this.timestamp = timestamp;
	}
	
	public Reward(PacketReward packet) {
		username = packet.getUsername();
		timestamp = packet.getTimestamp();
	}
	
	public Reward(NBTTagCompound nbt) {
		this(nbt.getString("username"), nbt.getLong("time"));
	}

	public String getUsername() {
		return username;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result
				+ ((username == null) ? 0 : username.toLowerCase().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Reward other = (Reward) obj;
		if (timestamp != other.timestamp)
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equalsIgnoreCase(other.username))
			return false;
		return true;
	}

	public NBTTagCompound writeToNbt() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("username", username);
		nbt.setLong("time", timestamp);
		return nbt;
	}
}
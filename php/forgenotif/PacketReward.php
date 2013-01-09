<?php
class PacketReward extends FNSPacket {

	private $reward;
	
	public function __construct($reward) {
		$this->reward = $reward;
	}

	protected function writeData() {
		$this->writeString($this->reward->getUsername());
		$this->writeLong($this->reward->getTimestamp());
	}
	
	protected function getPacketName() {
		return "reward";
	}
}
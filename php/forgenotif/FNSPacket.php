<?php
abstract class FNSPacket {
	
	private $buffer = '';
	
	protected function writeLong($long) {
		$left = $long & 0xffffffff00000000;
		$right = $long & 0x00000000ffffffff;
		$this->buffer .= pack('NN', $left, $right);
	}
	
	protected function writeShort($short) {
		$this->buffer .= pack('n', $short);
	}
	
	protected function writeString($string) {
		$this->writeShort(strlen($string));
		$this->buffer .= $string;
	}
	
	protected abstract function writeData();
	
	protected abstract function getPacketName();
	
	public function writeAndGetData($key, $closeConnection = true) {
		$this->writeString("1.0");
		$this->writeString($this->getPacketName());
		$this->writeData();
		
		$this->buffer .= chr($closeConnection ? 0 : 1);
		
		$sha1 = sha1($this->buffer, true);
		
		$encryptedHash = '';
		
		if (!openssl_private_encrypt($sha1, $encryptedHash, $key)) {
			return false;
		}			
	
		$this->buffer = $encryptedHash . $this->buffer;
		
		$this->buffer = pack('n', strlen($this->buffer)) . $this->buffer;
	
		return $this->buffer;
	}
}
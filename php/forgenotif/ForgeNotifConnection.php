<?php
class ForgeNotifConnection {

private $host;
private $port;
private $socket = false;
private $connected = false;

public function __construct($host, $port) {
	$this->host = $host;
	$this->port = $port;
}

public function create() {
	$this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
	return $this->socket !== false;
}

public function connect($host = false, $port = false) {
	if ($this->socket === false) {
		throw new Exception('Socket not created!');
	} else {
		if ($host !== false and $port !== false) {
			$this->host = $host;
			$this->port = $port;
		}
		$this->connected = socket_connect($this->socket, $this->host, $this->port);
		return $this->connected;
	}
}

public function sendPacket($packet, $key) {
	if (!$this->connected) {
		throw new Exception('Socket not connected!');
	}
	$buffer = $packet->writeAndGetData($key);
	$result = socket_write($this->socket, $buffer);
	return $result !== false && $result == strlen($buffer);
}
} ?>
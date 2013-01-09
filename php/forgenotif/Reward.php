<?php 

class Reward {
	
	private $username;
	private $timestamp;
	
	public function __construct($username, $timestamp = false) {
		if ($timestamp === false) {
			$timestamp = time();
		}
		$this->timestamp = $timestamp;
		$this->username = $username;
	}
	
	public function getUsername() {
		return $this->username;
	}
	
	public function getTimestamp() {
		return $this->timestamp;
	}
}
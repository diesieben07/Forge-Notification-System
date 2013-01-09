<?php

//$connection = new ForgeNotifConnection

$privateKey = loadKey('private.key');

if ($privateKey === false) {
	echo 'Failed to load key!';
	exit();
}

$packet = new PacketReward(new Reward('notch'));

$connection = new ForgeNotifConnection('127.0.0.1', 26311);

if (!$connection->create()) {
	echo 'could not create socket';
	exit;
}
if (!$connection->connect()) {
	echo 'could not connect to remote';
	exit;
}
if (!$connection->sendPacket($packet, $privateKey)) {
	echo 'could not send packet';
	exit;
}


function loadKey($file) {
	return openssl_pkey_get_private(pkcs8_to_pem(file_get_contents($file)));
}

function pkcs8_to_pem($der) {
    $BEGIN_MARKER = "-----BEGIN PRIVATE KEY-----";
    $END_MARKER = "-----END PRIVATE KEY-----";

    $value = base64_encode($der);

    $pem = $BEGIN_MARKER . "\n";
    $pem .= chunk_split($value, 64, "\n");
    $pem .= $END_MARKER . "\n";
    return $pem;
}

function __autoload($classname) {
	$path = 'forgenotif/' . $classname . '.php';
	require $path;
}
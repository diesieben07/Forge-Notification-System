package de.take_weiland.forgenotif.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.Configuration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.WorldAccessContainer;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import de.take_weiland.forgenotif.PacketTickHandler;
import de.take_weiland.forgenotif.Reward;
import de.take_weiland.forgenotif.network.ListenerThread;

public class ForgeNotifModContainer extends DummyModContainer implements WorldAccessContainer {

	private static final int DEFAULT_PORT = 26311;
	
	private static ForgeNotifModContainer instance = null;
	
	private final Logger logger;
	
	private int port;
	private ListenerThread listenerThread = null;
	private PacketTickHandler tickHandler;
	
	public Configuration config;
	
	public ForgeNotifModContainer() {
		super(new ModMetadata());
		if (instance != null) {
			throw new IllegalStateException("Cannot load the mod twice...");
		} else {
			instance = this;
		}
		ModMetadata meta = getMetadata();
		meta.modId = "FNS";
		meta.name = "Forge Notification System";
		
		logger = Logger.getLogger("FNS");
		logger.setParent(FMLLog.getLogger());
		logger.setUseParentHandlers(true);
	}
	
	public static ForgeNotifModContainer instance() {
		return instance;
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		if (!FMLCommonHandler.instance().getSide().isServer()) {
			logger.warning("This mod will only run on a Dedicated Minecraft Server! Aborting loading...");
			return false;
		} else {
			bus.register(this);
			return true;
		}
	}
	
	@Subscribe
	public void preInit(FMLPreInitializationEvent evt) {
		config = new Configuration(evt.getSuggestedConfigurationFile());
		config.load();
		
		port = config.get(Configuration.CATEGORY_GENERAL, "port", DEFAULT_PORT, "The TCP Port FNS will listen on").getInt();
		
		config.save();
		
		tickHandler = new PacketTickHandler(MinecraftServer.getServer(), logger);
		TickRegistry.registerScheduledTickHandler(tickHandler, Side.SERVER);
	}
	
	public PacketTickHandler getTickHandler() {
		return tickHandler; 
	}
	
	@Subscribe
	public void serverStarted(FMLServerStartedEvent evt) {
		if (listenerThread != null) {
			logger.info("Multiple server startings? Seems weird...");
			listenerThread.shutdown();
			listenerThread = null;
		}
		
		PublicKey key = null;
		
		try {
			key = loadKey(MinecraftServer.getServer());
		} catch (Exception e1) {
			logger.warning("public.key file missing or corrupted! Aborting...");
		}
		
		if (key == null) {
			return;
		}
		
		try {
			listenerThread = new ListenerThread(port, InetAddress.getByName(MinecraftServer.getServer().getHostname()), key, logger);
			listenerThread.start();
		} catch (UnknownHostException e) {
			logger.warning("Invalid host in server.properties!");
		} catch (IOException e) {
			logger.warning("IOException during Port binding! FNS will not run.");
			e.printStackTrace();
		}
		
		if (listenerThread == null) {
			return;
		}
	}
	
	private PublicKey loadKey(MinecraftServer server) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		File keyFile = server.getFile("public.key");
		InputStream fileStream = new BufferedInputStream(new FileInputStream(keyFile));
		byte[] bytes = new byte[(int)keyFile.length()];
		fileStream.read(bytes);
		fileStream.close();
		KeySpec spec = new X509EncodedKeySpec(bytes);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		return factory.generatePublic(spec);
	}
		
	@Subscribe
	public void serverStopping(FMLServerStoppingEvent evt) {
		if (listenerThread != null) {
			listenerThread.shutdown();
			while (listenerThread.isAlive()) {}
			tickHandler.stopProcessing();
			while (tickHandler.isProcessing()) {}
		}
	}
	
	@Override
	public NBTTagCompound getDataForWriting(SaveHandler handler, WorldInfo info) {
		NBTTagCompound pendingRewards = new NBTTagCompound();
		tickHandler.writeRewards(pendingRewards);
		return pendingRewards;
	}

	@Override
	public void readData(SaveHandler handler, WorldInfo info, Map<String, NBTBase> propertyMap, NBTTagCompound tag) {
		tickHandler.readRewards(tag);
	}
}
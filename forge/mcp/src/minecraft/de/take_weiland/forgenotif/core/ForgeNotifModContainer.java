package de.take_weiland.forgenotif.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import de.take_weiland.forgenotif.PacketTickHandler;
import de.take_weiland.forgenotif.network.ListenerThread;

public class ForgeNotifModContainer extends DummyModContainer implements WorldAccessContainer {

	private static final int DEFAULT_PORT = 26311;
	
	private static ForgeNotifModContainer instance = null;
	
	private final Logger logger;
	
	private boolean isInitialized = false;
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
		
		isInitialized = true;
	}
	
	public PacketTickHandler getTickHandler() {
		return tickHandler; 
	}
	
	@Subscribe
	public void serverStarted(FMLServerStartedEvent evt) {
		if (listenerThread != null) {
			logger.info("Multiple server startings? Seems weird...");
			listenerThread.shutdown();
		}
		try {
			listenerThread = new ListenerThread(port, InetAddress.getByName(MinecraftServer.getServer().getHostname()), logger);
			listenerThread.start();
		} catch (UnknownHostException e) {
			logger.warning("Invalid host in server.properties!");
		} catch (IOException e) {
			logger.warning("IOException during Port binding! FNS will not run.");
			e.printStackTrace();
		}
		
		tickHandler = new PacketTickHandler(MinecraftServer.getServer());
		TickRegistry.registerScheduledTickHandler(tickHandler, Side.SERVER);
	}
	
	@Subscribe
	public void serverStopping(FMLServerStoppingEvent evt) {
		if (listenerThread != null) {
			listenerThread.shutdown();
			while (listenerThread.isAlive()) {}
		}
	}

	@Override
	public NBTTagCompound getDataForWriting(SaveHandler handler, WorldInfo info) {
		return new NBTTagCompound();
	}

	@Override
	public void readData(SaveHandler handler, WorldInfo info, Map<String, NBTBase> propertyMap, NBTTagCompound tag) {
		
	}
}
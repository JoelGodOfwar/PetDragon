package com.ericdebouwer.petdragon;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;


public class PetDragon extends JavaPlugin  {
	
	//SUPPORTED:
	// 1.15, 1.15.1 (not tested), 1.15.2 (1.15-R1)
	// 1.16.1, 1.16.2
	//1.14.4, 1.14.x(not tested)
	
	public String logPrefix;
	private ConfigManager configManager;
	private DragonFactory dragonFactory;
	
	@Override
	public void onEnable(){
		this.logPrefix = "[" + this.getName() + "] "; 
		
		this.dragonFactory = new DragonFactory(this);
		
		if (!dragonFactory.isCorrectVersion()){
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" +ChatColor.RED + logPrefix + "Unsupported minecraft version! Check the download page for supported versions!");
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" +ChatColor.RED + logPrefix +"Plugin will disable to prevent crashing!");
			return;
		}
		
		this.configManager = new ConfigManager(this);
		
		if (!this.configManager.isValid()){
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" +ChatColor.RED + logPrefix +"Invalid config.yml, plugin will disable to prevent crashing!");
 			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" + ChatColor.RED + logPrefix + "See the header of the config.yml about fixing the problem.");
			return;
		}
		getServer().getConsoleSender().sendMessage(logPrefix +"Configuration has been successfully loaded!");
		
		new DragonCommand(this);
		DragonEvents dragonEvents = new DragonEvents(this);
		getServer().getPluginManager().registerEvents(dragonEvents, this);
	}
	
	public ConfigManager getConfigManager(){
		return this.configManager;
	}
	
	public DragonFactory getFactory(){
		return this.dragonFactory;
	}
	
	
	
}

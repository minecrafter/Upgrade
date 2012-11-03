package com.tropicalwikis.tuxcraft.plugins.upgrade;

/*
 * Copyright (c) 2012, tuxed
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of tuxed nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TUXED BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class UpgradePlugin extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;
    private static Permission perms = null;

    @Override
    public void onEnable() {
        if (!setupEconomy() ) {
            log.info("Vault not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        this.getDataFolder().mkdirs();
        // Determine if we are using the old file structure
        /*
        if(getConfig().contains("ranks")) {
        	log.info("You are using the old config.yml format which only supported one ladder. 0.2 introduced ladder support.");
        	log.info("Converting your config.yml to the new format...");
        	convertFromBukkitYaml();
        }*/
        if(!getConfig().contains("ranks")) {
        	log.severe("Adding default configuration. This may not be acceptable.");
        	log.severe("Visit http://tuxcraft.tropicalwikis.com/wiki/Upgrade/Configuration for configuration details.");
        	ArrayList<String> defaultRanks = new ArrayList<String>();
        	defaultRanks.add("Default");
        	defaultRanks.add("User");
        	defaultRanks.add("Member");
        	defaultRanks.add("Builder");
        	getConfig().set("ranks", defaultRanks);
        	getConfig().set("rankprices.Default", 0.0);
        	getConfig().set("rankprices.User", 10000.0);
        	getConfig().set("rankprices.Member", 25000.0);
        	getConfig().set("rankprices.Builder", 50000.0);
        	saveConfig();
        }
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        try {
        	if(sender.hasPermission("upgrade.reload") && args[0] == "reload") {
        		reloadConfig();
        		sender.sendMessage(ChatColor.GREEN + "Plugin reloaded!");
        		return true;
        	}
        } catch (Exception e){};
    	
    	if(!(sender instanceof Player)) {
            sender.sendMessage("This is a in-game command.");
            return true;
        }

    	Player player = (Player)sender;
        // tiem to rank upppppppp
        String c = perms.getPrimaryGroup(player);
        List<String> groups = getConfig().getStringList("ranks");
        if(groups.contains(c) && sender.hasPermission("upgrade.rankup")) {
    		String s = "";
    		try {
    			s = groups.get(groups.indexOf(c) + 1);
    		} catch (IndexOutOfBoundsException e) {
    			sender.sendMessage(ChatColor.RED + "You're at the highest rank already!");
    			return true;
    		}
    		double price = getConfig().getDouble("rankprices."+s, 1000.0);
    		if(econ.getBalance(sender.getName()) >= price) {
    			econ.withdrawPlayer(sender.getName(), price);
    			// Now, do one of the following:
    			// 1. If the command is "vault", attempt to change permissions using Vault
    			// 2. Otherwise invoke the command; %username% and %group% will automatically be filled in.
    			String cmd = getConfig().getString("promotion-command", "vault");
    			if(cmd.equals("vault")) {
    				perms.playerAddGroup(player, s);
    				// better be safer than sorry
    				perms.playerRemoveGroup(player, c);
    			} else {
    				getServer().dispatchCommand(getServer().getConsoleSender(), cmd.replaceAll("%username%", player.getName()).replaceAll("%group%", s));
    			}
    			if(getConfig().getBoolean("broadcast-promotions", true)) {
    				getServer().broadcastMessage(ChatColor.GREEN + sender.getName() + " has been promoted to "+s+"!");
    			}
    		} else {
    			sender.sendMessage(ChatColor.RED + "You do not have enough money. The next rank, "+s+", costs "+econ.format(price)+".");
    		}
        } else {
        	sender.sendMessage(ChatColor.RED + "You do not have permission to rank up.");
        }
        return true;
    }
}
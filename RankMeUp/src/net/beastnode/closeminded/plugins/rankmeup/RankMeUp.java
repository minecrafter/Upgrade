package net.beastnode.closeminded.plugins.rankmeup;

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

public class RankMeUp extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    public static Economy econ = null;
    public static Permission perms = null;
    
    @Override
    public void onEnable() {
        if (!setupEconomy() ) {
            log.info(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        this.getDataFolder().mkdirs();
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
        if(!(sender instanceof Player)) {
            sender.sendMessage("This is a in-game command.");
            return true;
        }

        Player player = (Player) sender;

        // tiem to rank upppppppp
        String c = perms.getPrimaryGroup(player);
        List<String> groups = getConfig().getStringList("ranks");
        if(groups.contains(c) && sender.hasPermission("rankmeup.rankup")) {
        	if(groups.contains(c)) {
        		String s = "";
        		try {
        			s = groups.get(groups.indexOf(c) + 1);
        		} catch (IndexOutOfBoundsException e) {
        			sender.sendMessage(ChatColor.RED + "You're at the highest rank already!");
        			return true;
        		}
        		double price = getConfig().getDouble("rankprices."+s);
        		if(econ.getBalance(sender.getName()) >= price) {
        			econ.withdrawPlayer(sender.getName(), price);
        			// Now, do one of the following:
        			// 1. If the command is "vault", attempt to change permissions using Vault
        			// 2. Otherwise invoke the command; %username% and %group% will automatically be filled in.
        			String cmd = getConfig().getString("promotion-command");
        			if(cmd.equals("vault")) {
        				perms.playerAddGroup(player, s);
        				// better be safer than sorry
        				perms.playerRemoveGroup(player, c);
        			} else {
        				getServer().dispatchCommand(getServer().getConsoleSender(), cmd.replaceAll("%username%", player.getName()).replaceAll("%group%", s));
        			}
        		} else {
        			sender.sendMessage(ChatColor.RED + "You do not have enough money. The next rank, "+s+", costs "+econ.format(price)+".");
        		}
        	}
        } else {
        	sender.sendMessage(ChatColor.RED + "You are not able to rank up.");
        }
        return true;
    }
}
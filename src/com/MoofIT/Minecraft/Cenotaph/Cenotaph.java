package com.MoofIT.Minecraft.Cenotaph;

/**
 * Cenotaph - A Dead Man's Chest plugin for Bukkit
 * By Jim Drey (Southpaw018) <moof@moofit.com>
 * Original Copyright (C) 2011 Steven "Drakia" Scott <Drakia@Gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Spider;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import org.yi.acru.bukkit.Lockette.Lockette;

/*
TODO 2.2 release
	- code refactor
	- register integration
	- cenotaph payment
	- improved override messages
	- improved timing messages
*/

public class Cenotaph extends JavaPlugin {
	private final eListener entityListener = new eListener();
	private final bListener blockListener = new bListener();
	private final sListener serverListener = new sListener();
	private final pListener playerListener = new pListener();
	public static Logger log;
	PluginManager pm;

	private LWCPlugin lwcPlugin = null;
	private Lockette LockettePlugin = null;

	private ConcurrentLinkedQueue<TombBlock> tombList = new ConcurrentLinkedQueue<TombBlock>();
	private HashMap<Location, TombBlock> tombBlockList = new HashMap<Location, TombBlock>();
	private HashMap<String, ArrayList<TombBlock>> playerTombList = new HashMap<String, ArrayList<TombBlock>>();
	private HashMap<String, EntityDamageEvent> deathCause = new HashMap<String, EntityDamageEvent>();
	private FileConfiguration config;
	private Cenotaph plugin;


	/**
	 * Configuration options - Defaults
	 */
	//Core
	private boolean logEvents = false;
	private boolean cenotaphSign = true;
	private boolean noDestroy = false;
	private boolean pMessage = true;
	private boolean saveCenotaphList = true;
	private boolean noInterfere = true;
	private boolean versionCheck = true;
	private boolean voidCheck = true;
	private boolean creeperProtection = false;
	private String signMessage[] = new String[] {
		"{name}",
		"RIP",
		"{date}",
		"{time}"
	};
	private String dateFormat = "MM/dd/yyyy";
	private String timeFormat = "hh:mm a";
	private List<String> disableInWorlds;

	//Removal
	private boolean destroyQuickLoot = false;
	private boolean cenotaphRemove = false;
	private int removeTime = 3600;
	private boolean removeWhenEmpty = false;
	private boolean keepUntilEmpty = false;
	private boolean levelBasedRemoval = false;
	private int levelBasedTime = 300;

	//Security
	private boolean LocketteEnable = true;
	private boolean lwcEnable = false;
	private boolean securityRemove = false;
	private int securityTimeout = 3600;
	private boolean lwcPublic = false;

	//DeathMessages
	private HashMap<String, Object> deathMessages = new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put("Monster.Zombie", "a Zombie");
			put("Monster.Skeleton", "a Skeleton");
			put("Monster.Spider", "a Spider");
			put("Monster.Wolf", "a Wolf");
			put("Monster.Creeper", "a Creeper");
			put("Monster.Slime", "a Slime");
			put("Monster.Ghast", "a Ghast");
			put("Monster.PigZombie", "a Pig Zombie");
			put("Monster.Giant", "a Giant");
			put("Monster.Other", "a Monster");
	
			put("World.Cactus", "a Cactus");
			put("World.Suffocation", "Suffocation");
			put("World.Fall", "a Fall");
			put("World.Fire", "a Fire");
			put("World.Burning", "Burning");
			put("World.Lava", "Lava");
			put("World.Drowning", "Drowning");
			put("World.Lightning", "Lightning");
	
			put("Explosion.Misc", "an Explosion");
			put("Explosion.TNT", "a TNT Explosion");
	
			put("Misc.Dispenser", "a Dispenser");
			put("Misc.Void", "the Void");
			put("Misc.Other", "Unknown");
		}
	};

	//Config versioning
	private int configVer = 0;
	private final int configCurrent = 12;

	public void onEnable() {
		PluginDescriptionFile pdfFile = getDescription();
		log = Logger.getLogger("Minecraft");

		String thisVersion = pdfFile.getVersion();
		log.info(pdfFile.getName() + " v." + thisVersion + " is enabled.");

		pm = getServer().getPluginManager();

		pm.registerEvents(entityListener,this);
		pm.registerEvents(blockListener,this);
		pm.registerEvents(playerListener,this);
		pm.registerEvents(serverListener,this);

		lwcPlugin = (LWCPlugin)checkPlugin("LWC");
		LockettePlugin = (Lockette)checkPlugin("Lockette");
		plugin = this;

		loadConfig();
		for (World w : getServer().getWorlds())
			loadTombList(w.getName());

		if (versionCheck) {
			versionCheck(true);
		}

		// Start removal timer. Run every 5 seconds (20 ticks per second)
		if (securityRemove || cenotaphRemove)
			getServer().getScheduler().scheduleSyncRepeatingTask(this, new TombThread(), 0L, 100L);
	}

	public void loadConfig() {
		this.reloadConfig();
		config = this.getConfig();

		configVer = config.getInt("configVer", configVer);
		if (configVer == 0) {
			log.info("[Cenotaph] Configuration error or no config file found. Generating default config file.");
			saveDefaultConfig();
			this.reloadConfig(); //hack to force good data into configs TODO 2.2: proper defaults
			config = this.getConfig();			
		}
		else if (configVer < configCurrent) {
			log.warning("[Cenotaph] Your config file is out of date! Delete your config and /cenadmin reload to see the new options. Proceeding using set options from config file and defaults for new options..." );
		}

		//Core
		logEvents = config.getBoolean("Core.logEvents", logEvents);
		cenotaphSign = config.getBoolean("Core.cenotaphSign", cenotaphSign);
		noDestroy = config.getBoolean("Core.noDestroy", noDestroy);
		pMessage = config.getBoolean("Core.playerMessage", pMessage);
		saveCenotaphList = config.getBoolean("Core.saveCenotaphList", saveCenotaphList);
		noInterfere = config.getBoolean("Core.noInterfere", noInterfere);
		versionCheck = config.getBoolean("Core.versionCheck", versionCheck);
		voidCheck = config.getBoolean("Core.voidCheck", voidCheck);
		creeperProtection = config.getBoolean("Core.creeperProtection", creeperProtection);
		signMessage = loadSign();
		dateFormat = config.getString("Core.Sign.dateFormat", dateFormat);
		timeFormat = config.getString("Core.Sign.timeFormat", timeFormat);

		try {
			disableInWorlds = config.getStringList("Core.disableInWorlds");
		} catch (NullPointerException e) {
			log.warning("[Cenotaph] Configuration failure while loading disableInWorlds. Using defaults.");
		}		

		//Removal
		destroyQuickLoot = config.getBoolean("Removal.destroyQuickLoot", destroyQuickLoot);
		cenotaphRemove = config.getBoolean("Removal.cenotaphRemove", cenotaphRemove);
		removeTime = config.getInt("Removal.removeTime", removeTime);
		removeWhenEmpty = config.getBoolean("Removal.removeWhenEmpty", removeWhenEmpty);
		keepUntilEmpty = config.getBoolean("Removal.keepUntilEmpty", keepUntilEmpty);
		levelBasedRemoval = config.getBoolean("Removal.levelBasedRemoval", levelBasedRemoval);
		levelBasedTime = config.getInt("Removal.levelBasedTime", levelBasedTime);

		//Security
		LocketteEnable = config.getBoolean("Security.LocketteEnable", LocketteEnable);
		lwcEnable = config.getBoolean("Security.lwcEnable", lwcEnable);
		securityRemove = config.getBoolean("Security.securityRemove", securityRemove);
		securityTimeout = config.getInt("Security.securityTimeout", securityTimeout);
		lwcPublic = config.getBoolean("Security.lwcPublic", lwcPublic);

		//DeathMessages
		try {
			deathMessages = (HashMap<String, Object>)config.getConfigurationSection("DeathMessages").getValues(true);
		} catch (NullPointerException e) {
			log.warning("[Cenotaph] Configuration failure while loading deathMessages. Using defaults.");
		}
	}

	public void loadTombList(String world) {
		if (!saveCenotaphList) return;
		try {
			File fh = new File(this.getDataFolder().getPath(), "tombList-" + world + ".db");
			if (!fh.exists()) return;
			Scanner scanner = new Scanner(fh);
			while (scanner.hasNextLine()) { //TODO handle bad entry cases 
				String line = scanner.nextLine().trim();
				String[] split = line.split(":");
				//block:lblock:sign:owner:level:time:lwc
				Block block = readBlock(split[0]);
				Block lBlock = readBlock(split[1]);
				Block sign = readBlock(split[2]);
				String owner = split[3];

				//hacking in level handling for 2.1 2/14/12. remove in a few months?
				/*int level = Integer.valueOf(split[4]);
				long time = Long.valueOf(split[5]);
				boolean lwc = Boolean.valueOf(split[6]);*/
				int level = 0;
				long time = 0;
				boolean lwc = false;
				
				if (split.length == 6) {
					level = Integer.valueOf(split[4]);
					time = Long.valueOf(split[5]);					
					
				}
				else {
					time = Long.valueOf(split[4]);					
					lwc = Boolean.valueOf(split[5]);					
				}
				//end hack
				if (block == null || owner == null) {
					log.info("[Cenotaph] Invalid entry in database " + fh.getName());
					continue;
				}
				TombBlock tBlock = new TombBlock(block, lBlock, sign, owner, level, time, lwc);
				tombList.offer(tBlock);
				// Used for quick tombStone lookup
				tombBlockList.put(block.getLocation(), tBlock);
				if (lBlock != null) tombBlockList.put(lBlock.getLocation(), tBlock);
				if (sign != null) tombBlockList.put(sign.getLocation(), tBlock);
				ArrayList<TombBlock> pList = playerTombList.get(owner);
				if (pList == null) {
					pList = new ArrayList<TombBlock>();
					playerTombList.put(owner, pList);
				}
				pList.add(tBlock);
			}
			scanner.close();
		} catch (IOException e) {
			log.info("[Cenotaph] Error loading cenotaph list: " + e);
		}
	}

	public void saveCenotaphList(String world) {
		if (!saveCenotaphList) return;
		try {
			File fh = new File(this.getDataFolder().getPath(), "tombList-" + world + ".db");
			BufferedWriter bw = new BufferedWriter(new FileWriter(fh));
			for (Iterator<TombBlock> iter = tombList.iterator(); iter.hasNext();) {
				TombBlock tBlock = iter.next();
				// Skip not this world
				if (!tBlock.getBlock().getWorld().getName().equalsIgnoreCase(world)) continue;

				StringBuilder builder = new StringBuilder();

				bw.append(printBlock(tBlock.getBlock()));
				bw.append(":");
				bw.append(printBlock(tBlock.getLBlock()));
				bw.append(":");
				bw.append(printBlock(tBlock.getSign()));
				bw.append(":");
				bw.append(tBlock.getOwner());
				bw.append(":");
				bw.append(Integer.toString(tBlock.getOwnerLevel()));
				bw.append(":");				
				bw.append(String.valueOf(tBlock.getTime()));
				bw.append(":");
				bw.append(String.valueOf(tBlock.getLwcEnabled()));

				bw.append(builder.toString());
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			log.info("[Cenotaph] Error saving cenotaph list: " + e);
		}
	}

	private String printBlock(Block b) {
		if (b == null) return "";
		return b.getWorld().getName() + "," + b.getX() + "," + b.getY() + "," + b.getZ();
	}

	private Block readBlock(String b) {
		if (b.length() == 0) return null;
		String[] split = b.split(",");
		//world,x,y,z
		World world = getServer().getWorld(split[0]);
		if (world == null) return null;
		return world.getBlockAt(Integer.valueOf(split[1]), Integer.valueOf(split[2]), Integer.valueOf(split[3]));
	}

	public void onDisable() {
		for (World w : getServer().getWorlds())
			saveCenotaphList(w.getName());
	}
	private String[] loadSign() {
		String[] msg = signMessage;
		msg[0] = config.getString("Core.Sign.Line1", signMessage[0]);
		msg[1] = config.getString("Core.Sign.Line2", signMessage[1]);
		msg[2] = config.getString("Core.Sign.Line3", signMessage[2]);
		msg[3] = config.getString("Core.Sign.Line4", signMessage[3]);
		return msg;
	}

	/*
	 * Check if a plugin is loaded/enabled already. Returns the plugin if so, null otherwise
	 */
	private Plugin checkPlugin(String p) {
		Plugin plugin = pm.getPlugin(p);
		return checkPlugin(plugin);
	}

	private Plugin checkPlugin(Plugin plugin) {
		if (plugin != null && plugin.isEnabled()) {
			log.info("[Cenotaph] Using " + plugin.getDescription().getName() + " (v" + plugin.getDescription().getVersion() + ")");
			return plugin;
		}
		return null;
	}

	private Boolean activateLWC(Player player, TombBlock tBlock) {
		if (!lwcEnable) return false;
		if (lwcPlugin == null) return false;
		LWC lwc = lwcPlugin.getLWC();

		// Register the chest + sign as private
		Block block = tBlock.getBlock();
		Block sign = tBlock.getSign();
		lwc.getPhysicalDatabase().registerProtection(block.getTypeId(), Protection.Type.PRIVATE, block.getWorld().getName(), player.getName(), "", block.getX(), block.getY(), block.getZ());
		if (sign != null)
			lwc.getPhysicalDatabase().registerProtection(sign.getTypeId(), Protection.Type.PRIVATE, block.getWorld().getName(), player.getName(), "", sign.getX(), sign.getY(), sign.getZ());

		tBlock.setLwcEnabled(true);
		return true;
	}

	private Boolean protectWithLockette(Player player, TombBlock tBlock) {
		if (!LocketteEnable) return false;
		if (LockettePlugin == null) return false;

		Block signBlock = null;

		signBlock = findPlace(tBlock.getBlock(),true);
		if (signBlock == null) {
			sendMessage(player, "No room for Lockette sign! Chest unsecured!");
			return false;
		}

		signBlock.setType(Material.AIR); //hack to prevent oddness with signs popping out of the ground as of Bukkit 818
		signBlock.setType(Material.WALL_SIGN);

		String facing = getDirection((getYawTo(signBlock.getLocation(),tBlock.getBlock().getLocation()) + 270) % 360);
		if (facing == "East")
			signBlock.setData((byte)0x02);
		else if (facing == "West")
			signBlock.setData((byte)0x03);
		else if (facing == "North")
			signBlock.setData((byte)0x04);
		else if (facing == "South")
			signBlock.setData((byte)0x05);
		else {
			sendMessage(player, "Error placing Lockette sign! Chest unsecured!");
			return false;
		}

		BlockState signBlockState = null;
		signBlockState = signBlock.getState();
		final Sign sign = (Sign)signBlockState;

		String name = player.getName();
		if (name.length() > 15) name = name.substring(0, 15);
		sign.setLine(0, "[Private]");
		sign.setLine(1, name);
		getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				sign.update();
			}
		});
		tBlock.setLocketteSign(sign);
		return true;
	}

	public void deactivateLWC(TombBlock tBlock, boolean force) {
		if (!lwcEnable) return;
		if (lwcPlugin == null) return;
		LWC lwc = lwcPlugin.getLWC();

		// Remove the protection on the chest
		Block _block = tBlock.getBlock();
		Protection protection = lwc.findProtection(_block);
		if (protection != null) {
			lwc.getPhysicalDatabase().removeProtection(protection.getId());
			//Set to public instead of removing completely
			if (lwcPublic && !force)
				lwc.getPhysicalDatabase().registerProtection(_block.getTypeId(), Protection.Type.PUBLIC, _block.getWorld().getName(), tBlock.getOwner(), "", _block.getX(), _block.getY(), _block.getZ());
		}

		// Remove the protection on the sign
		_block = tBlock.getSign();
		if (_block != null) {
			protection = lwc.findProtection(_block);
			if (protection != null) {
				protection.remove();
				// Set to public instead of removing completely
				if (lwcPublic && !force)
					lwc.getPhysicalDatabase().registerProtection(_block.getTypeId(), Protection.Type.PUBLIC, _block.getWorld().getName(), tBlock.getOwner(), "", _block.getX(), _block.getY(), _block.getZ());
			}
		}
		tBlock.setLwcEnabled(false);
	}
	public void deactivateLockette(TombBlock tBlock) {
		if (tBlock.getLocketteSign() == null) return;
		tBlock.getLocketteSign().getBlock().setType(Material.AIR);
		tBlock.removeLocketteSign();
	}

	private void removeTomb(TombBlock tBlock, boolean removeList) {
		if (tBlock == null) return;

		tombBlockList.remove(tBlock.getBlock().getLocation());
		if (tBlock.getLBlock() != null) tombBlockList.remove(tBlock.getLBlock().getLocation());
		if (tBlock.getSign() != null) tombBlockList.remove(tBlock.getSign().getLocation());

		// Remove just this tomb from tombList
		ArrayList<TombBlock> tList = playerTombList.get(tBlock.getOwner());
		if (tList != null) {
			tList.remove(tBlock);
			if (tList.size() == 0) {
				playerTombList.remove(tBlock.getOwner());
			}
		}

		if (removeList)
			tombList.remove(tBlock);

		if (tBlock.getBlock() != null)
			saveCenotaphList(tBlock.getBlock().getWorld().getName());
	}

	/*
	 * Check whether the player has the given permissions.
	 */
	public boolean hasPerm(Player player, String perm) {
		return player.hasPermission(perm);
	}

	public void sendMessage(Player p, String msg) {
		if (!pMessage) return;
		p.sendMessage("[Cenotaph] " + msg);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { //TODO needs major cleanup, move indexing to separate class function
		if (!(sender instanceof Player)) return false;
		Player p = (Player)sender;
		String cmd = command.getName();
		if (cmd.equalsIgnoreCase("cenlist")) {
			if (!hasPerm(p, "cenotaph.cmd.cenotaphlist")) {
				sendMessage(p, "Permission Denied");
				return true;
			}
			ArrayList<TombBlock> pList = playerTombList.get(p.getName());
			if (pList == null) {
				sendMessage(p, "You have no cenotaphs.");
				return true;
			}
			sendMessage(p, "Cenotaph List:");
			int i = 0;
			for (TombBlock tomb : pList) {
				i++;
				if (tomb.getBlock() == null) continue;
				int X = tomb.getBlock().getX();
				int Y = tomb.getBlock().getY();
				int Z = tomb.getBlock().getZ();
				sendMessage(p, " " + i + " - World: " + tomb.getBlock().getWorld().getName() + " @(" + X + "," + Y + "," + Z + ")");
			}
			return true;
		} else if (cmd.equalsIgnoreCase("cenfind")) {
			if (!hasPerm(p, "cenotaph.cmd.cenotaphfind")) {
				sendMessage(p, "Permission Denied");
				return true;
			}
			if (args.length != 1) return false;
			ArrayList<TombBlock> pList = playerTombList.get(p.getName());
			if (pList == null) {
				sendMessage(p, "You have no cenotaphs.");
				return true;
			}
			int slot = 0;
			try {
				slot = Integer.parseInt(args[0]);
			} catch (Exception e) {
				sendMessage(p, "Invalid cenotaph");
				return true;
			}
			slot -= 1;
			if (slot < 0 || slot >= pList.size()) {
				sendMessage(p, "Invalid cenotaph");
				return true;
			}
			TombBlock tBlock = pList.get(slot);
			double degrees = (getYawTo(tBlock.getBlock().getLocation(), p.getLocation()) + 270) % 360;
			p.setCompassTarget(tBlock.getBlock().getLocation());
			sendMessage(p, "Your cenotaph #" + args[0] + " is to the " + getDirection(degrees) + ". Your compass has been set to point at its location. Use /cenreset to reset it to your spawn point.");
			return true;
		} else if (cmd.equalsIgnoreCase("centime")) {
			if (!hasPerm(p, "cenotaph.cmd.cenotaphtime")) {
				sendMessage(p, "Permission Denied");
				return true;
			}
			if (args.length != 1) return false;
			ArrayList<TombBlock> pList = playerTombList.get(p.getName());
			if (pList == null) {
				sendMessage(p, "You have no cenotaphs.");
				return true;
			}
			int slot = 0;
			try {
				slot = Integer.parseInt(args[0]);
			} catch (Exception e) {
				sendMessage(p, "Invalid cenotaph");
				return true;
			}
			slot -= 1;
			if (slot < 0 || slot >= pList.size()) {
				sendMessage(p, "Invalid cenotaph");
				return true;
			}
			long cTime = System.currentTimeMillis() / 1000;
			TombBlock tBlock = pList.get(slot);
			long secTimeLeft = (tBlock.getTime() + securityTimeout) - cTime;
			long remTimeLeft = (tBlock.getTime() + removeTime) - cTime;

			if (securityRemove && secTimeLeft > 0) sendMessage(p, "Security will be removed from your cenotaph in " + secTimeLeft + " seconds.");

			if (cenotaphRemove & remTimeLeft > 0) sendMessage(p, "Your cenotaph will break in " + remTimeLeft + " seconds");
			if (removeWhenEmpty && keepUntilEmpty) sendMessage(p, "Break override: Your cenotaph will break when it is emptied, but will not break until then.");
			else {
				if (removeWhenEmpty) sendMessage(p, "Break override: Your cenotaph will break when it is emptied.");
				if (keepUntilEmpty) sendMessage(p, "Break override: Your cenotaph will not break until it is empty.");
			}

			return true;
		} else if (cmd.equalsIgnoreCase("cenreset")) {
			if (!hasPerm(p, "cenotaph.cmd.cenotaphreset")) {
				sendMessage(p, "Permission Denied");
				return true;
			}
			p.setCompassTarget(p.getWorld().getSpawnLocation());
			return true;
		}
		else if (cmd.equalsIgnoreCase("cenadmin")) {
			if (!hasPerm(p, "cenotaph.admin")) {
				sendMessage(p, "Permission Denied");
				return true;
			}
			if (args.length == 0) {
				sendMessage(p, "Usage: /cenadmin list"); //TODO 2.2 use name matching
				sendMessage(p, "Usage: /cenadmin list <playerCaseSensitive>");
				sendMessage(p, "Usage: /cenadmin find <playerCaseSensitive> <#>");
				sendMessage(p, "Usage: /cenadmin remove <playerCaseSensitive> <#>");
				sendMessage(p, "Usage: /cenadmin version");
				sendMessage(p, "Usage: /cenadmin reload");
				return true;
			}
			if (args[0].equalsIgnoreCase("list")) {
				if (!hasPerm(p, "cenotaph.admin.list")) {
					sendMessage(p, "Permission Denied");
					return true;
				}
				if (args.length < 2) {
					if (playerTombList.keySet().isEmpty()) {
						sendMessage(p, "There are no cenotaphs.");
						return true;
					}
					sendMessage(p, "Players with cenotaphs:");
					for (String player : playerTombList.keySet()) {
						sendMessage(p, player);
					}
					return true;
				}
				ArrayList<TombBlock> pList = playerTombList.get(args[1]);
				if (pList == null) {
					sendMessage(p, "No cenotaphs found for " + args[1] + ".");
					return true;
				}
				sendMessage(p, "Cenotaph List:");
				int i = 0;
				for (TombBlock tomb : pList) {
					i++;
					if (tomb.getBlock() == null) continue;
					int X = tomb.getBlock().getX();
					int Y = tomb.getBlock().getY();
					int Z = tomb.getBlock().getZ();
					sendMessage(p, " " + i + " - World: " + tomb.getBlock().getWorld().getName() + " @(" + X + "," + Y + "," + Z + ")");
				}
				return true;
			} else if (args[0].equalsIgnoreCase("find")) {
				if (!hasPerm(p, "cenotaph.admin.find")) {
					sendMessage(p, "Permission Denied");
					return true;
				}
				ArrayList<TombBlock> pList = playerTombList.get(args[1]);
				if (pList == null) {
					sendMessage(p, "No cenotaphs found for " + args[1] + ".");
					return true;
				}
				int slot = 0;
				try {
					slot = Integer.parseInt(args[2]);
				} catch (Exception e) {
					sendMessage(p, "Invalid cenotaph entry.");
					return true;
				}
				slot -= 1;
				if (slot < 0 || slot >= pList.size()) {
					sendMessage(p, "Invalid cenotaph entry.");
					return true;
				}
				TombBlock tBlock = pList.get(slot);
				double degrees = (getYawTo(tBlock.getBlock().getLocation(), p.getLocation()) + 270) % 360;
				int X = tBlock.getBlock().getX();
				int Y = tBlock.getBlock().getY();
				int Z = tBlock.getBlock().getZ();
				sendMessage(p, args[1] + "'s cenotaph #" + args[2] + " is at " + X + "," + Y + "," + Z + ", to the " + getDirection(degrees) + ".");
				return true;
			} else if (args[0].equalsIgnoreCase("time")) {
				if (!hasPerm(p, "cenotaph.admin.cenotaphtime")) {
					sendMessage(p, "Permission Denied");
					return true;
				}
				if (args.length != 3) return false;
				ArrayList<TombBlock> pList = playerTombList.get(args[1]);
				if (pList == null) {
					sendMessage(p, "No cenotaphs found for " + args[1] + ".");
					return true;
				}
				int slot = 0;
				try {
					slot = Integer.parseInt(args[2]);
				} catch (Exception e) {
					sendMessage(p, "Invalid cenotaph entry.");
					return true;
				}
				slot -= 1;
				if (slot < 0 || slot >= pList.size()) {
					sendMessage(p, "Invalid cenotaph entry.");
					return true;
				}
				long cTime = System.currentTimeMillis() / 1000;
				TombBlock tBlock = pList.get(slot);
				long secTimeLeft = (tBlock.getTime() + securityTimeout) - cTime;
				long remTimeLeft = (tBlock.getTime() + removeTime) - cTime;
				if (securityRemove && secTimeLeft > 0) sendMessage(p, "Security removal: " + secTimeLeft + " seconds.");
				if (cenotaphRemove & remTimeLeft > 0) sendMessage(p, "Cenotaph removal: " + remTimeLeft + " seconds.");
				if (keepUntilEmpty || removeWhenEmpty) sendMessage(p, "Keep until empty:" + keepUntilEmpty + "; remove when empty: " + removeWhenEmpty);
				return true;
			} else if (args[0].equalsIgnoreCase("version")) {
				String message;
				message = versionCheck(false);
				sendMessage(p, message);

				if (configVer == 0) {
					sendMessage(p, "Using default config.");
				}
				else if (configVer < configCurrent) {
					sendMessage(p, "Your config file is out of date.");
				}
				else if (configVer == configCurrent) {
					sendMessage(p, "Your config file is up to date.");
				}
			} else if (args[0].equalsIgnoreCase("remove")) {
				if (!hasPerm(p, "cenotaph.admin.remove")) {
					sendMessage(p, "Permission Denied");
					return true;
				}
				ArrayList<TombBlock> pList = playerTombList.get(args[1]);
				if (pList == null) {
					sendMessage(p, "No cenotaphs found for " + args[1] + ".");
					return true;
				}
				int slot = 0;
				try {
					slot = Integer.parseInt(args[2]);
				} catch (Exception e) {
					sendMessage(p, "Invalid cenotaph entry.");
					return true;
				}
				slot -= 1;
				if (slot < 0 || slot >= pList.size()) {
					sendMessage(p, "Invalid cenotaph entry.");
					return true;
				}
				TombBlock tBlock = pList.get(slot);
				destroyCenotaph(tBlock);
			} else if (args[0].equalsIgnoreCase("reload")) {
				if (!hasPerm(p, "cenotaph.admin.reload")) {
					sendMessage(p, "Permission Denied");
					return true;
				}
				loadConfig();
				log.info("[Cenotaph] Configuration reloaded from file.");
				sendMessage(p, "Configuration reloaded from file.");
			} else {
				sendMessage(p, "Usage: /cenadmin list");
				sendMessage(p, "Usage: /cenadmin list <playerCaseSensitive>");
				sendMessage(p, "Usage: /cenadmin find <playerCaseSensitive> <#>");
				sendMessage(p, "Usage: /cenadmin remove <playerCaseSensitive> <#>");
				sendMessage(p, "Usage: /cenadmin version");
				return true;
			}
			return true;
		}
		return false;
	}

	public String versionCheck(Boolean printToLog) {
		String thisVersion = getDescription().getVersion();
		URL url = null;
		try {
			url = new URL("http://www.moofit.com/minecraft/cenotaph.ver?v=" + thisVersion);
			BufferedReader in = null;
			in = new BufferedReader(new InputStreamReader(url.openStream()));
			String newVersion = "";
			String line;
			while ((line = in.readLine()) != null) {
				newVersion += line;
			}
			in.close();
			if (!newVersion.equals(thisVersion)) {
				if (printToLog) log.warning("[Cenotaph] Cenotaph is out of date! This version: " + thisVersion + "; latest version: " + newVersion + ".");
				return "Cenotaph is out of date! This version: " + thisVersion + "; latest version: " + newVersion + ".";
			}
			else {
				if (printToLog) log.info("[Cenotaph] Cenotaph is up to date at version " + thisVersion + ".");
				return "Cenotaph is up to date at version " + thisVersion + ".";
			}
		}
		catch (MalformedURLException ex) {
			if (printToLog) log.warning("[Cenotaph] Error accessing update URL.");
			return "Error accessing update URL.";
		}
		catch (IOException ex) {
			if (printToLog) log.warning("[Cenotaph] Error checking for update.");
			return "Error checking for update.";
		}
	}

	/**
	 * Gets the Yaw from one location to another in relation to North.
	 *
	 */
	public double getYawTo(Location from, Location to) {
			final int distX = to.getBlockX() - from.getBlockX();
			final int distZ = to.getBlockZ() - from.getBlockZ();
			double degrees = Math.toDegrees(Math.atan2(-distX, distZ));
			degrees += 180;
		return degrees;
	}

	/**
	 * Converts a rotation to a cardinal direction name.
	 * Author: sk89q - Original function from CommandBook plugin
	 * @param rot
	 * @return
	 */
	private static String getDirection(double rot) {
		if (0 <= rot && rot < 22.5) {
			return "North";
		} else if (22.5 <= rot && rot < 67.5) {
			return "Northeast";
		} else if (67.5 <= rot && rot < 112.5) {
			return "East";
		} else if (112.5 <= rot && rot < 157.5) {
			return "Southeast";
		} else if (157.5 <= rot && rot < 202.5) {
			return "South";
		} else if (202.5 <= rot && rot < 247.5) {
			return "Southwest";
		} else if (247.5 <= rot && rot < 292.5) {
			return "West";
		} else if (292.5 <= rot && rot < 337.5) {
			return "Northwest";
		} else if (337.5 <= rot && rot < 360.0) {
			return "North";
		} else {
			return null;
		}
	}

	/**
	 *
	 * Print a message to terminal if logEvents is enabled
	 * @param msg
	 * @return
	 *
	 */
	private void logEvent(String msg) {
		if (!logEvents) return;
		log.info("[Cenotaph] " + msg);
	}

	public class bListener implements Listener {
		@EventHandler
		public void onBlockBreak(BlockBreakEvent event) {
			Block b = event.getBlock();
			Player p = event.getPlayer();

			if (b.getType() == Material.WALL_SIGN)
			{
				org.bukkit.material.Sign signData = (org.bukkit.material.Sign)b.getState().getData();
				TombBlock tBlock = tombBlockList.get(b.getRelative(signData.getAttachedFace()).getLocation());
				if (tBlock == null) return;

				if (tBlock.getLocketteSign() != null) {
					Sign sign = (Sign)b.getState();
					event.setCancelled(true);
					sign.update();
					return;
				}
			}

			if (b.getType() != Material.CHEST && b.getType() != Material.SIGN_POST) return;

			TombBlock tBlock = tombBlockList.get(b.getLocation());
			if (tBlock == null) return;

			if (noDestroy && !hasPerm(p, "cenotaph.admin")) {
				logEvent(p.getName() + " tried to destroy cenotaph at " + b.getLocation());
				sendMessage(p, "Cenotaph unable to be destroyed");
				event.setCancelled(true);
				return;
			}

			if (lwcPlugin != null && lwcEnable && tBlock.getLwcEnabled()) {
				if (tBlock.getOwner().equals(p.getName()) || hasPerm(p, "cenotaph.admin")) {
					deactivateLWC(tBlock, true);
				} else {
					event.setCancelled(true);
					return;
				}
			}
			logEvent(p.getName() + " destroyed cenotaph at " + b.getLocation());
			removeTomb(tBlock, true);
		}
	}

	public class pListener implements Listener {
		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerInteract(PlayerInteractEvent event) {
			if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
			Block b = event.getClickedBlock();
			if (b.getType() != Material.SIGN_POST && b.getType() != Material.CHEST) return;
			// We'll do quickloot on rightclick of chest if we're going to destroy it anyways
			if (b.getType() == Material.CHEST && (!destroyQuickLoot || !noDestroy)) return;
			if (!hasPerm(event.getPlayer(), "cenotaph.quickloot")) return;

			TombBlock tBlock = tombBlockList.get(b.getLocation());
			if (tBlock == null || !(tBlock.getBlock().getState() instanceof Chest)) return;

			if (!tBlock.getOwner().equals(event.getPlayer().getName())) return;

			Chest sChest = (Chest)tBlock.getBlock().getState();
			Chest lChest = (tBlock.getLBlock() != null) ? (Chest)tBlock.getLBlock().getState() : null;

			ItemStack[] items = sChest.getInventory().getContents();
			boolean overflow = false;
			for (int cSlot = 0; cSlot < items.length; cSlot++) {
				ItemStack item = items[cSlot];
				if (item == null) continue;
				if (item.getType() == Material.AIR) continue;
				int slot = event.getPlayer().getInventory().firstEmpty();
				if (slot == -1) {
					overflow = true;
					break;
				}
				event.getPlayer().getInventory().setItem(slot, item);
				sChest.getInventory().clear(cSlot);
			}
			if (lChest != null) {
				items = lChest.getInventory().getContents();
				for (int cSlot = 0; cSlot < items.length; cSlot++) {
					ItemStack item = items[cSlot];
					if (item == null) continue;
					if (item.getType() == Material.AIR) continue;
					int slot = event.getPlayer().getInventory().firstEmpty();
					if (slot == -1) {
						overflow = true;
						break;
					}
					event.getPlayer().getInventory().setItem(slot, item);
					lChest.getInventory().clear(cSlot);
				}
			}

			sendMessage(event.getPlayer(), "Cenotaph quicklooted!");
			logEvent(event.getPlayer() + " quicklooted cenotaph at " + tBlock.getBlock().getLocation());

			if (!overflow) {
				// We're quicklooting, so no need to resume this interaction
				event.setUseInteractedBlock(Result.DENY);
				event.setUseItemInHand(Result.DENY); //TODO: Minor bug here - if you're holding a sign, it'll still pop up
				event.setCancelled(true);

				if (destroyQuickLoot) {
					destroyCenotaph(tBlock);
				}
			}

			// Manually update inventory for the time being.
			event.getPlayer().updateInventory();
		}
	}

	public class eListener implements Listener
	{
		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityDamage(EntityDamageEvent event) {
			if (event.isCancelled()) return;
			if (!(event.getEntity() instanceof Player))return;

			Player player = (Player)event.getEntity();
			// Add them to the list if they're about to die
			if (player.getHealth() - event.getDamage() <= 0) {
				deathCause.put(player.getName(), event);
			}
		}

		@EventHandler
		public void onEntityExplode(EntityExplodeEvent event)
		{
			if (event.isCancelled()) return;
			if (!creeperProtection) return;
			for (Block block : event.blockList()) {
				TombBlock tBlock = tombBlockList.get(block.getLocation());
				if (tBlock != null) {
					event.setCancelled(true);
				}
			}
		}

		@EventHandler
		public void onEntityDeath(EntityDeathEvent event)
		{
			if (!(event.getEntity() instanceof Player)) return;
			Player p = (Player)event.getEntity();

			if (!hasPerm(p, "cenotaph.use")) return;

			logEvent(p.getName() + " died.");

			if (event.getDrops().size() == 0) {
				sendMessage(p, "Inventory Empty.");
				logEvent(p.getName() + " inventory empty.");
				return;
			}

			for (String world : disableInWorlds) {
				String curWorld = p.getWorld().getName();
				if (world.equalsIgnoreCase(curWorld)) {
					sendMessage(p,"Cenotaphs are disabled in " + curWorld + ". Inventory dropped.");
					logEvent(p.getName() + " died in " + curWorld + " and did not receive a cenotaph.");
					return;
				}
			}


			// Get the current player location.
			Location loc = p.getLocation();
			Block block = p.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

			// If we run into something we don't want to destroy, go one up.
			if (	block.getType() == Material.STEP ||
					block.getType() == Material.TORCH ||
					block.getType() == Material.REDSTONE_WIRE ||
					block.getType() == Material.RAILS ||
					block.getType() == Material.STONE_PLATE ||
					block.getType() == Material.WOOD_PLATE ||
					block.getType() == Material.REDSTONE_TORCH_ON ||
					block.getType() == Material.REDSTONE_TORCH_OFF ||
					block.getType() == Material.CAKE_BLOCK) {
				block = p.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
			}

			//Don't create the chest if it or its sign would be in the void
			if (voidCheck && ((cenotaphSign && block.getY() > 126) || (!cenotaphSign && block.getY() > 127) || p.getLocation().getY() < 1)) {
				sendMessage(p, "Your Cenotaph would be in the Void. Inventory dropped.");
				logEvent(p.getName() + " died in the Void.");
				return;
			}

			// Check if the player has a chest.
			int pChestCount = 0;
			int pSignCount = 0;
			for (ItemStack item : event.getDrops()) {
				if (item == null) continue;
				if (item.getType() == Material.CHEST) pChestCount += item.getAmount();
				if (item.getType() == Material.SIGN) pSignCount += item.getAmount();
			}

			if (pChestCount == 0 && !hasPerm(p, "cenotaph.freechest")) {
				sendMessage(p, "No chest found in inventory. Inventory dropped.");
				logEvent(p.getName() + " No chest in inventory.");
				return;
			}

			// Check if we can replace the block.
			block = findPlace(block,false);
			if ( block == null ) {
				sendMessage(p, "Could not find room for chest. Inventory dropped.");
				logEvent(p.getName() + " Could not find room for chest.");
				return;
			}

			// Check if there is a nearby chest
			if (noInterfere && checkChest(block)) {
				sendMessage(p, "There is a chest interfering with your cenotaph. Inventory dropped.");
				logEvent(p.getName() + " Chest interfered with cenotaph creation.");
				return;
			}

			int removeChestCount = 1;
			int removeSignCount = 0;

			// Do the check for a large chest block here so we can check for interference
			Block lBlock = findLarge(block);

			// Set the current block to a chest, init some variables for later use.
			block.setType(Material.CHEST);
			// We're running into issues with 1.3 where we can't cast to a Chest :(
			BlockState state = block.getState();
			if (!(state instanceof Chest)) {
				sendMessage(p, "Could not access chest. Inventory dropped.");
				logEvent(p.getName() + " Could not access chest.");
				return;
			}
			Chest sChest = (Chest)state;
			Chest lChest = null;
			int slot = 0;
			int maxSlot = sChest.getInventory().getSize();

			// Check if they need a large chest.
			if (event.getDrops().size() > maxSlot) {
				// If they are allowed spawn a large chest to catch their entire inventory.
				if (lBlock != null && hasPerm(p, "cenotaph.large")) {
					removeChestCount = 2;
					// Check if the player has enough chests
					if (pChestCount >= removeChestCount || hasPerm(p, "cenotaph.freechest")) {
						lBlock.setType(Material.CHEST);
						lChest = (Chest)lBlock.getState();
						maxSlot = maxSlot * 2;
					} else {
						removeChestCount = 1;
					}
				}
			}

			// Don't remove any chests if they get a free one.
			if (hasPerm(p, "cenotaph.freechest"))
				removeChestCount = 0;

			// Check if we have signs enabled, if the player can use signs, and if the player has a sign or gets a free sign
			Block sBlock = null;
			if (cenotaphSign && hasPerm(p, "cenotaph.sign") &&
				(pSignCount > 0 || hasPerm(p, "cenotaph.freesign"))) {
				// Find a place to put the sign, then place the sign.
				sBlock = sChest.getWorld().getBlockAt(sChest.getX(), sChest.getY() + 1, sChest.getZ());
				if (canReplace(sBlock.getType())) {
					createSign(sBlock, p);
					removeSignCount += 1;
				} else if (lChest != null) {
					sBlock = lChest.getWorld().getBlockAt(lChest.getX(), lChest.getY() + 1, lChest.getZ());
					if (canReplace(sBlock.getType())) {
						createSign(sBlock, p);
						removeSignCount += 1;
					}
				}
			}

			// Don't remove a sign if they get a free one
			if (hasPerm(p, "cenotaph.freesign"))
				removeSignCount -= 1;

			// Create a TombBlock for this tombstone
			TombBlock tBlock = new TombBlock(sChest.getBlock(), (lChest != null) ? lChest.getBlock() : null, sBlock, p.getName(), p.getLevel() + 1, (System.currentTimeMillis() / 1000));

			// Protect the chest/sign if LWC is installed.
			Boolean prot = false;
			Boolean protLWC = false;
			if (hasPerm(p, "cenotaph.lwc"))
				prot = activateLWC(p, tBlock);
			tBlock.setLwcEnabled(prot);
			if (prot) protLWC = true;

			// Protect the chest with Lockette if installed, enabled, and unprotected.
			if (hasPerm(p, "cenotaph.lockette")) {
				if (hasPerm(p, "cenotaph.freelockettesign")) {
					prot = protectWithLockette(p, tBlock);
				} else if (pSignCount > removeSignCount) {
					removeSignCount += 1;
					prot = protectWithLockette(p, tBlock);
				}
			}
			// Add tombstone to list
			tombList.offer(tBlock);

			// Add tombstone blocks to tombBlockList
			tombBlockList.put(tBlock.getBlock().getLocation(), tBlock);
			if (tBlock.getLBlock() != null) tombBlockList.put(tBlock.getLBlock().getLocation(), tBlock);
			if (tBlock.getSign() != null) tombBlockList.put(tBlock.getSign().getLocation(), tBlock);

			// Add tombstone to player lookup list
			ArrayList<TombBlock> pList = playerTombList.get(p.getName());
			if (pList == null) {
				pList = new ArrayList<TombBlock>();
				playerTombList.put(p.getName(), pList);
			}
			pList.add(tBlock);

			saveCenotaphList(p.getWorld().getName());

			// Next get the players inventory using the getDrops() method.
			for (Iterator<ItemStack> iter = event.getDrops().listIterator(); iter.hasNext();) {
				ItemStack item = iter.next();
				if (item == null) continue;
				// Take the chest(s)
				if (removeChestCount > 0 && item.getType() == Material.CHEST) {
					if (item.getAmount() >= removeChestCount) {
						item.setAmount(item.getAmount() - removeChestCount);
						removeChestCount = 0;
					} else {
						removeChestCount -= item.getAmount();
						item.setAmount(0);
					}
					if (item.getAmount() == 0) {
						iter.remove();
						continue;
					}
				}

				// Take a sign
				if (removeSignCount > 0 && item.getType() == Material.SIGN){
					item.setAmount(item.getAmount() - 1);
					removeSignCount -= 1;
					if (item.getAmount() == 0) {
						iter.remove();
						continue;
					}
				}

				// Add items to chest if not full.
				if (slot < maxSlot) {
					if (slot >= sChest.getInventory().getSize()) {
						if (lChest == null) continue;
						lChest.getInventory().setItem(slot % sChest.getInventory().getSize(), item);
					} else {
						sChest.getInventory().setItem(slot, item);
					}
					iter.remove();
					slot++;
				} else if (removeChestCount == 0) break;
			}

			// Tell the player how many items went into chest.
			String msg = "Inventory stored in chest. "; //TODO 2.2 clean up this mess
			if (event.getDrops().size() > 0)
				msg += event.getDrops().size() + " items wouldn't fit in chest.";
			sendMessage(p, msg);
			logEvent(p.getName() + " " + msg);
			if (prot && protLWC) {
				sendMessage(p, "Chest protected with LWC. " + securityTimeout + "s before chest is unprotected.");
				logEvent(p.getName() + " Chest protected with LWC. " + securityTimeout + "s before chest is unprotected.");
			}
			if (prot && !protLWC) {
				sendMessage(p, "Chest protected with Lockette. " + securityTimeout + "s before chest is unprotected.");
				logEvent(p.getName() + " Chest protected with Lockette.");
			}
			if (cenotaphRemove) {
				sendMessage(p, "Chest will break in " + (levelBasedRemoval ? Math.min(p.getLevel() + 1 * levelBasedTime,removeTime) : removeTime) + "s unless an override is specified.");
				logEvent(p.getName() + " Chest will break in " + removeTime + "s");
			}
			if (removeWhenEmpty && keepUntilEmpty) sendMessage(p, "Break override: Your cenotaph will break when it is emptied, but will not break until then.");
			else {
				if (removeWhenEmpty) sendMessage(p, "Break override: Your cenotaph will break when it is emptied.");
				if (keepUntilEmpty) sendMessage(p, "Break override: Your cenotaph will not break until it is empty.");
			}
		}

		private void createSign(Block signBlock, Player p) {
			String date = new SimpleDateFormat(dateFormat).format(new Date());
			String time = new SimpleDateFormat(timeFormat).format(new Date());
			String name = p.getName();
			String reason = "Unknown";

			EntityDamageEvent dmg = deathCause.get(name);
			if (dmg != null) {
				deathCause.remove(name);
				reason = getCause(dmg);
			}

			signBlock.setType(Material.SIGN_POST);
			final Sign sign = (Sign)signBlock.getState();

			for (int x = 0; x < 4; x++) {
				String line = signMessage[x];
				line = line.replace("{name}", name);
				line = line.replace("{date}", date);
				line = line.replace("{time}", time);
				line = line.replace("{reason}", reason);

				if (line.length() > 15) line = line.substring(0, 15);
				sign.setLine(x, line);
			}

			getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() {
					sign.update();
				}
			});
		}

		private String getCause(EntityDamageEvent dmg) {
			switch (dmg.getCause()) {
				case ENTITY_ATTACK:
				{
					EntityDamageByEntityEvent event = (EntityDamageByEntityEvent)dmg;
					Entity e = event.getDamager();
					if (e == null) {
						return deathMessages.get("Misc.Dispenser").toString();
					} else if (e instanceof Player) {
						return ((Player) e).getDisplayName();
					} else if (e instanceof PigZombie) {
						return deathMessages.get("Monster.PigZombie").toString();
					} else if (e instanceof Giant) {
						return deathMessages.get("Monster.Giant").toString();
					} else if (e instanceof Zombie) {
						return deathMessages.get("Monster.Zombie").toString();
					} else if (e instanceof Skeleton) {
						return deathMessages.get("Monster.Skeleton").toString();
					} else if (e instanceof Spider) {
						return deathMessages.get("Monster.Spider").toString();
					} else if (e instanceof Creeper) {
						return deathMessages.get("Monster.Creeper").toString();
					} else if (e instanceof Ghast) {
						return deathMessages.get("Monster.Ghast").toString();
					} else if (e instanceof Slime) {
						return deathMessages.get("Monster.Slime").toString();
					} else if (e instanceof Wolf) {
						return deathMessages.get("Monster.Wolf").toString();
					} else {
						return deathMessages.get("Monster.Other").toString();
					}
				}
				case CONTACT:
					return deathMessages.get("World.Cactus").toString();
				case SUFFOCATION:
					return deathMessages.get("World.Suffocation").toString();
				case FALL:
					return deathMessages.get("World.Fall").toString();
				case FIRE:
					return deathMessages.get("World.Fire").toString();
				case FIRE_TICK:
					return deathMessages.get("World.Burning").toString();
				case LAVA:
					return deathMessages.get("World.Lava").toString();
				case DROWNING:
					return deathMessages.get("World.Drowning").toString();
				case BLOCK_EXPLOSION:
					return deathMessages.get("Explosion.Misc").toString();
				case ENTITY_EXPLOSION:
				{
					try {
						EntityDamageByEntityEvent event = (EntityDamageByEntityEvent)dmg;
						Entity e = event.getDamager();
						if (e instanceof TNTPrimed) return deathMessages.get("Explosion.TNT").toString();
						else if (e instanceof Fireball) return deathMessages.get("Monster.Ghast").toString();
						else return deathMessages.get("Monster.Creeper").toString();
					} catch (Exception e) {
						return deathMessages.get("Explosion.Misc").toString();
					}
				}
				case VOID:
					return deathMessages.get("Misc.Void").toString();
				case LIGHTNING:
					return deathMessages.get("World.Lightning").toString();
				default:
					return deathMessages.get("Misc.Other").toString();
			}
		}

		Block findLarge(Block base) {
			// Check all 4 sides for air.
			Block exp;
			exp = base.getWorld().getBlockAt(base.getX() - 1, base.getY(), base.getZ());
			if (canReplace(exp.getType()) && (!noInterfere || !checkChest(exp))) return exp;
			exp = base.getWorld().getBlockAt(base.getX(), base.getY(), base.getZ() - 1);
			if (canReplace(exp.getType()) && (!noInterfere || !checkChest(exp))) return exp;
			exp = base.getWorld().getBlockAt(base.getX() + 1, base.getY(), base.getZ());
			if (canReplace(exp.getType()) && (!noInterfere || !checkChest(exp))) return exp;
			exp = base.getWorld().getBlockAt(base.getX(), base.getY(), base.getZ() + 1);
			if (canReplace(exp.getType()) && (!noInterfere || !checkChest(exp))) return exp;
			return null;
		}

		boolean checkChest(Block base) {
			// Check all 4 sides for a chest.
			Block exp;
			exp = base.getWorld().getBlockAt(base.getX() - 1, base.getY(), base.getZ());
			if (exp.getType() == Material.CHEST) return true;
			exp = base.getWorld().getBlockAt(base.getX(), base.getY(), base.getZ() - 1);
			if (exp.getType() == Material.CHEST) return true;
			exp = base.getWorld().getBlockAt(base.getX() + 1, base.getY(), base.getZ());
			if (exp.getType() == Material.CHEST) return true;
			exp = base.getWorld().getBlockAt(base.getX(), base.getY(), base.getZ() + 1);
			if (exp.getType() == Material.CHEST) return true;
			return false;
		}
	}


	/**
	 * Find a block near the base block to place the tombstone
	 * @param base
	 * @return
	 */
	Block findPlace(Block base, Boolean CardinalSearch) {
		if (canReplace(base.getType())) return base;
		int baseX = base.getX();
		int baseY = base.getY();
		int baseZ = base.getZ();
		World w = base.getWorld();

		if (CardinalSearch) {
			Block b;
			b = w.getBlockAt(baseX - 1, baseY, baseZ);
			if (canReplace(b.getType())) return b;
			b = w.getBlockAt(baseX + 1, baseY, baseZ);
			if (canReplace(b.getType())) return b;
			b = w.getBlockAt(baseX, baseY, baseZ - 1);
			if (canReplace(b.getType())) return b;
			b = w.getBlockAt(baseX, baseY, baseZ + 1);
			if (canReplace(b.getType())) return b;
			b = w.getBlockAt(baseX, baseY, baseZ);
			if (canReplace(b.getType())) return b;

			return null;
		}

		for (int x = baseX - 1; x < baseX + 1; x++) {
			for (int z = baseZ - 1; z < baseZ + 1; z++) {
				Block b = w.getBlockAt(x, baseY, z);
				if (canReplace(b.getType())) return b;
			}
		}

		return null;
	}

	Boolean canReplace(Material mat) {
		return (mat == Material.AIR ||
				mat == Material.SAPLING ||
				mat == Material.WATER ||
				mat == Material.STATIONARY_WATER ||
				mat == Material.LAVA ||
				mat == Material.STATIONARY_LAVA ||
				mat == Material.YELLOW_FLOWER ||
				mat == Material.RED_ROSE ||
				mat == Material.BROWN_MUSHROOM ||
				mat == Material.RED_MUSHROOM ||
				mat == Material.FIRE ||
				mat == Material.CROPS ||
				mat == Material.SNOW ||
				mat == Material.SUGAR_CANE ||
				mat == Material.GRAVEL ||
				mat == Material.SAND);
	}

	public String convertTime(long s) { //TODO implement later
		long days = s / 86400;
		int hours = (int)(s % 86400 / 3600);
		int minutes = (int)(s % 86400 % 3600 / 60);
		int seconds = (int)(s % 86400 % 3600 % 60);
		return
			(days > 1 ? days : "")
			+ (hours < 10 ? "0" : "") + hours
			+ ":" + (minutes < 10 ? "0" : "") + minutes
			+ ":" + (seconds< 10 ? "0" : "") + seconds;
	}

	public class sListener implements Listener {
		@EventHandler(priority = EventPriority.MONITOR)
		public void onPluginEnable(PluginEnableEvent event) {
			if (lwcPlugin == null) {
				if (event.getPlugin().getDescription().getName().equalsIgnoreCase("LWC")) {
					lwcPlugin = (LWCPlugin)checkPlugin(event.getPlugin());
				}
			}
			if (LockettePlugin == null) {
				if (event.getPlugin().getDescription().getName().equalsIgnoreCase("Lockette")) {
					LockettePlugin = (Lockette)checkPlugin(event.getPlugin());
				}
			}
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPluginDisable(PluginDisableEvent event) {
			if (event.getPlugin() == lwcPlugin) {
				log.info("[Cenotaph] LWC plugin lost.");
				lwcPlugin = null;
			}
			if (event.getPlugin() == LockettePlugin) {
				log.info("[Cenotaph] Lockette plugin lost.");
				LockettePlugin = null;
			}
		}
	}

	private class TombThread extends Thread {
		public void run() {
			long cTime = System.currentTimeMillis() / 1000;
			for (Iterator<TombBlock> iter = tombList.iterator(); iter.hasNext();) {
				TombBlock tBlock = iter.next();

				//"empty" option checks
				if (keepUntilEmpty || removeWhenEmpty) {
					if (tBlock.getBlock().getState() instanceof Chest) {
						int itemCount = 0;

						Chest sChest = (Chest)tBlock.getBlock().getState();
						Chest lChest = (tBlock.getLBlock() != null) ? (Chest)tBlock.getLBlock().getState() : null;

						for (ItemStack item : sChest.getInventory().getContents()) {
							if (item != null) itemCount += item.getAmount();
						}
						if (lChest != null && itemCount == 0) {
							for (ItemStack item : lChest.getInventory().getContents()) {
								if (item != null) itemCount += item.getAmount();
							}
						}

						if (keepUntilEmpty) {
							if (itemCount > 0) continue;
						}
						if (removeWhenEmpty) {
							if (itemCount == 0) destroyCenotaph(tBlock);
							iter.remove();
						}
					}
				}

				//Security removal check
				if (securityRemove) {
					Player p = getServer().getPlayer(tBlock.getOwner());

					if (cTime >= (tBlock.getTime() + securityTimeout)) {
						if (tBlock.getLwcEnabled() && lwcPlugin != null) {
							deactivateLWC(tBlock, false);
							tBlock.setLwcEnabled(false);
							if (p != null)
								sendMessage(p, "LWC protection disabled on your cenotaph!");
						}
						if (tBlock.getLocketteSign() != null && LockettePlugin != null) {
							deactivateLockette(tBlock);
							if (p != null)
								sendMessage(p, "Lockette protection disabled on your cenotaph!");
						}
					}
				}
				//Block removal check
				if (cenotaphRemove) {
					if (levelBasedRemoval) {
						if (cTime > Math.min(tBlock.getTime() + tBlock.getOwnerLevel() * levelBasedTime, tBlock.getTime() + removeTime)) {
							destroyCenotaph(tBlock);
							iter.remove();
						}
					}
					else {
						if (cTime > (tBlock.getTime() + removeTime)) {
							destroyCenotaph(tBlock);
							iter.remove();							
						}
					}
				}
			}
		}
	}

	public void destroyCenotaph(Location loc) {
		destroyCenotaph(tombBlockList.get(loc));
	}
	public void destroyCenotaph(TombBlock tBlock) {
		if (tBlock.getBlock().getChunk().load() == false) {
			log.severe("[Cenotaph] Error loading world chunk trying to remove cenotaph at " + tBlock.getBlock().getX() + "," + tBlock.getBlock().getY() + "," + tBlock.getBlock().getZ() + " owned by " + tBlock.getOwner() + ".");
			return;
		}
		if (tBlock.getSign() != null) tBlock.getSign().setType(Material.AIR);
		deactivateLockette(tBlock);
		deactivateLWC(tBlock, true);

		tBlock.getBlock().setType(Material.AIR);
		if (tBlock.getLBlock() != null) tBlock.getLBlock().setType(Material.AIR);

		removeTomb(tBlock, true);

		Player p = getServer().getPlayer(tBlock.getOwner());
		if (p != null)
			sendMessage(p, "Your cenotaph has been destroyed!");
	}

	public class TombBlock {
		private Block block;
		private Block lBlock;
		private Block sign;
		private Sign LocketteSign;
		private long time;
		private String owner;
		private int ownerLevel;
		private boolean lwcEnabled = false;

		TombBlock(Block block, Block lBlock, Block sign, String owner, int ownerLevel, long time) {
			this.block = block;
			this.lBlock = lBlock;
			this.sign = sign;
			this.owner = owner;
			this.ownerLevel = ownerLevel;
			this.time = time;
		}
		TombBlock(Block block, Block lBlock, Block sign, String owner, int ownerLevel, long time, boolean lwc) {
			this.block = block;
			this.lBlock = lBlock;
			this.sign = sign;
			this.owner = owner;
			this.ownerLevel = ownerLevel;
			this.time = time;
			this.lwcEnabled = lwc;
		}
		
		long getTime() {
			return time;
		}
		Block getBlock() {
			return block;
		}
		Block getLBlock() {
			return lBlock;
		}
		Block getSign() {
			return sign;
		}
		Sign getLocketteSign() {
			return LocketteSign;
		}
		String getOwner() {
			return owner;
		}
		int getOwnerLevel() {
			return ownerLevel;
		}
		boolean getLwcEnabled() {
			return lwcEnabled;
		}
		void setLwcEnabled(boolean val) {
			lwcEnabled = val;
		}
		void setLocketteSign(Sign sign) {
			this.LocketteSign = sign;
		}
		void removeLocketteSign() {
			this.LocketteSign = null;
		}
	}
	public HashMap<String, ArrayList<TombBlock>> getCenotaphList() {
		return playerTombList;
	}
}

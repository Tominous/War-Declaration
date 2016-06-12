package me.axiom.bpl.wardeclaration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;

public class WarDeclaration extends JavaPlugin {
	
	Logger logger = Logger.getLogger("minecraft");
	PluginDescriptionFile pdf = this.getDescription();
	
	public File factionWarsFile = new File(getDataFolder() + "/FactionWars.yml");
	public YamlConfiguration factionWars = YamlConfiguration.loadConfiguration(factionWarsFile);
	
	public File factionWarsLogFile = new File(getDataFolder() + "/FactionsWarLog.yml");
	public YamlConfiguration factionWarLog = YamlConfiguration.loadConfiguration(factionWarsLogFile);
	
	// HashSet<PlayerStats information>
	public HashSet<PlayerStats> playerStats = new HashSet<PlayerStats>();
	
	public HashSet<Faction> engagedWars = new HashSet<Faction>();
	
	public HashMap<Faction, Faction> factionVictoryDecider = new HashMap<Faction, Faction>();
	
	public void onEnable() {
		
		if (!(getDataFolder()).exists()) {
			logger.info("[" + pdf.getName() + " v" + pdf.getVersion() + "] attempting to create plugin data folder.");
            getDataFolder().mkdir();
            logger.info("[" + pdf.getName() + " v" + pdf.getVersion() + "] plugin data folder has been successfully created.");
        }
		
		if (!factionWarsFile.exists()) {
			try {
				logger.info("[" + pdf.getName() + " v" + pdf.getVersion() + "] attempting to create war status file.");
				factionWarsFile.createNewFile();
				logger.info("[" + pdf.getName() + " v" + pdf.getVersion() + "] war status file has been successfully created.");
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
		
		if (!factionWarsLogFile.exists()) {
			try {
				logger.info("[" + pdf.getName() + " v" + pdf.getVersion() + "] attempting to war log file.");
				factionWarsLogFile.createNewFile();
				logger.info("[" + pdf.getName() + " v" + pdf.getVersion() + "] war log has been successfully created.");
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
		
		getCommand("war").setExecutor(new CmdExecutor(this));
		
		getServer().getPluginManager().registerEvents(new LoginListener(this), this);
		getServer().getPluginManager().registerEvents(new FactionNameChangeListener(this), this);
		getServer().getPluginManager().registerEvents(new FactionCreationListener(this), this);

		initialiseFactionWarsFile();
		initialiseEngagedWars();
		initialisePlayerStats();
		
		logger.info("[" + pdf.getName() + " v" + pdf.getVersion() + "] has been successfully enabled.");
		
	}
	
	public void onDisable() {
		
		saveFactionWarsFile();
		saveFactionWarsLogFile();
		
		logger.info("[" + pdf.getName() + " v" + pdf.getVersion() + "] has been disabled.");
		
	}
	
	public void initialisePlayerStats() {
		
		for (Player p : getServer().getOnlinePlayers()) {
			PlayerStats pS = new PlayerStats(p, 0, 0);
			playerStats.add(pS);
		}
		
	}
	
	public PlayerStats getPlayerStats(Player p) {
		for (PlayerStats pS : playerStats) {
			if (pS.getPlayer() == p) {
				return pS;
			}
		}
		return null;
	}
	
	public void initialiseEngagedWars() {
		
		for (Faction f : FactionColl.get().getAll()) {
			if (factionWars.contains(f.getName())) {
				if (factionWars.getBoolean(f.getName() + ".Engaged")) {
					
				}
			}
		}
		
	}
	
	public void initialiseFactionWarsFile() {
		
		for (Faction f : FactionColl.get().getAll()) {
			if (!(factionWars.contains(f.getName())) && !(f.getName().equalsIgnoreCase("safezone")) && !(f.getName().equalsIgnoreCase("warzone")) && !(f.getName().equalsIgnoreCase(FactionColl.get().getByName("Wilderness").getName()))) {
				factionWars.set(f.getName() + ".Target", "none");
				factionWars.set(f.getName() + ".Requester", "none");
				factionWars.set(f.getName() + ".Status", "available");
				factionWars.set(f.getName() + ".Engaged", false);
				factionWars.set(f.getName() + ".ForfeitedBy", "none");
				factionWars.set(f.getName() + ".TimeOfDeclaration", 0);
			}
		}
		
		saveFactionWarsFile();
		
	}
	
	public void addWarLog(Faction winFaction, Faction loseFaction, String method) {
		
		int newKey = getMaxKey(factionWarLog) + 1;
		factionWarLog.set(String.valueOf(newKey) + ".Requester", getWarRequester(winFaction).getName());
		factionWarLog.set(String.valueOf(newKey) + ".Target", getWarTarget(winFaction).getName());
		factionWarLog.set(String.valueOf(newKey) + ".TimeOfDeclaration", factionWars.getLong(winFaction.getName() + ".TimeOfDeclaration"));
		factionWarLog.set(String.valueOf(newKey) + ".TimeOfEnd", System.currentTimeMillis());
		factionWarLog.set(String.valueOf(newKey) + ".Victory", winFaction.getName());
		factionWarLog.set(String.valueOf(newKey) + ".Defeat", loseFaction.getName());
		factionWarLog.set(String.valueOf(newKey) + ".Method", method); // Method being, forfeit, or ended.
		
		int winFactionTotalKills = 0;
		int winFactionTotalDeaths = 0;
		int loseFactionTotalKills = 0;
		int loseFactionTotalDeaths = 0;
		
		for (PlayerStats pS : playerStats) {
			MPlayer mP = MPlayer.get(pS.getPlayer());
			Faction f = mP.getFaction();
			
			if (f == winFaction || f == loseFaction) {
				factionWarLog.set(String.valueOf(newKey) + "." + f.getName() + "." + mP.getName() + ".Kills", pS.getKills());
				factionWarLog.set(String.valueOf(newKey) + "." + f.getName() + "." + mP.getName() + ".Deaths", pS.getDeaths());
				if (f == winFaction) {
					winFactionTotalKills = winFactionTotalKills + pS.getKills();
					winFactionTotalDeaths = winFactionTotalDeaths + pS.getDeaths();
				} else if (f == loseFaction) {
					loseFactionTotalKills = loseFactionTotalKills + pS.getKills();
					loseFactionTotalDeaths = loseFactionTotalDeaths + pS.getDeaths();
				}
			}
		}
		
		factionWarLog.set(String.valueOf(newKey) + ".VictoryKills", winFactionTotalKills);
		factionWarLog.set(String.valueOf(newKey) + ".VictoryDeaths", winFactionTotalDeaths);
		factionWarLog.set(String.valueOf(newKey) + ".DefeatKills", loseFactionTotalKills);
		factionWarLog.set(String.valueOf(newKey) + ".DefeatKills", loseFactionTotalKills);
		
	}
	
	public int getMaxKey(YamlConfiguration yml) {
		int maxKey = Integer.MAX_VALUE;
		for (String s : yml.getKeys(false)) {
			int number = Integer.parseInt(s);
			if (number > maxKey) {
				maxKey = number;
			}
		}
		return maxKey;
	}
	
	public void saveFactionWarsLogFile() {
		try {
			factionWarLog.save(factionWarsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveFactionWarsFile() {
		try {
			factionWars.save(factionWarsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Faction getWarRequester(Faction f) {
		if (factionWars.getString(f.getName() + ".Requester").equalsIgnoreCase("none")) {
			return null;
		} else {
			return FactionColl.get().getByName(factionWars.getString(f.getName() + ".Requester"));
		}
	}
	
	public Faction getWarTarget(Faction f) {
		if (factionWars.getString(f.getName() + ".Target").equalsIgnoreCase("none")) {
			return null;
		} else {
			return FactionColl.get().getByName(factionWars.getString(f.getName() + ".Target"));
		}
	}
	
}

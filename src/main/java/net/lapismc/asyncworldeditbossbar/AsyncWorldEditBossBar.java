package net.lapismc.asyncworldeditbossbar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.primesoft.asyncworldedit.api.IAsyncWorldEdit;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerEntry;
import org.primesoft.asyncworldedit.api.progressDisplay.IProgressDisplay;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class AsyncWorldEditBossBar extends JavaPlugin implements IProgressDisplay {

    //Storage for the boss bar associated with each player using AWE
    private final HashMap<UUID, BossBar> bossBars = new HashMap<>();
    //A list of the last ten times remaining for each player
    private final HashMap<UUID, List<Double>> previousTimeLeft = new HashMap<>();

    @Override
    public void onEnable() {
        //Save the config before the registration to make sure the variables there in will be available
        saveDefaultConfig();
        //Add this class as a progress display in the AsyncWorldEdit API
        IAsyncWorldEdit awe = (IAsyncWorldEdit) Bukkit.getPluginManager().getPlugin("AsyncWorldEdit");
        if (awe == null) {
            getLogger().severe("AWE not found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        awe.getProgressDisplayManager().registerProgressDisplay(this);
        getLogger().info(getName() + " v." + getDescription().getVersion() + " has been enabled");
    }

    @Override
    public void onDisable() {
        //Remove this class as a progress display in the AsyncWorldEdit API
        IAsyncWorldEdit awe = (IAsyncWorldEdit) Bukkit.getPluginManager().getPlugin("AsyncWorldEdit");
        if (awe == null)
            return;
        awe.getProgressDisplayManager().unregisterProgressDisplay(this);
    }

    @Override
    public void disableMessage(IPlayerEntry player) {
        //Ignore this if we dont have a bar for this player
        if (!bossBars.containsKey(player.getUUID()))
            return;
        //Get the players bar
        BossBar bossBar = bossBars.get(player.getUUID());
        //Set it to invisible
        bossBar.setVisible(false);
        //Delete the previous times so that the average doesnt bleed between jobs
        previousTimeLeft.remove(player.getUUID());
    }

    @Override
    public void setMessage(IPlayerEntry player, int jobsCount, int queuedBlocks, int maxQueuedBlocks, double timeLeft,
                           double placingSpeed, double percentage) {
        Player p = Bukkit.getPlayer(player.getUUID());
        if (p == null)
            return;
        //Get the stored boss bar or make a new one if we dont have one stored
        BossBar bossBar;
        if (bossBars.containsKey(player.getUUID())) {
            bossBar = bossBars.get(player.getUUID());
        } else {
            //Make it the color defined in the config or blue if they haven't set it
            BarColor color = BarColor.valueOf(getConfig().getString("BarColor", "BLUE"));
            bossBar = Bukkit.createBossBar("", color, BarStyle.SOLID);
            //Add the player to the bar, The title should be set so quickly that they shouldn't see the first update anyway
            bossBar.addPlayer(p);
        }
        //Add the player if they arent in the list, this might occur if the player goes offline
        if (!bossBar.getPlayers().contains(Bukkit.getPlayer(player.getUUID()))) {
            bossBar.addPlayer(p);
        }
        //The number we get is 0-100, we need 0-1, a simple /100 sorts that out
        double progress = Math.max(0, Math.min(1, percentage / 100));
        bossBar.setProgress(progress);
        //The doubles given to us are stupidly long, so we use this number format to shorten them
        NumberFormat nf = new DecimalFormat("#.##");
        String format = getConfig().getString("TitleFormat");

        //Process the last values for this player for TimeLeft to get an average
        //This smooths out sudden changes in the time left value over 10 updates of the value
        List<Double> oldTimeLeftValues;
        if (previousTimeLeft.containsKey(player.getUUID())) {
            oldTimeLeftValues = previousTimeLeft.get(player.getUUID());
        } else {
            oldTimeLeftValues = new ArrayList<>();
        }
        //Enter the new Value
        oldTimeLeftValues.add(timeLeft);
        //Trim the list if we have more than 10 values
        while (oldTimeLeftValues.size() > 10) {
            oldTimeLeftValues.remove(0);
        }
        //Calculate the average
        double total = oldTimeLeftValues.stream().mapToDouble(value -> value).sum();
        double smoothedTimeLeft = total / oldTimeLeftValues.size();

        //TODO: Use pretty time to make the time left better

        //Translate colors and set a default
        format = ChatColor.translateAlternateColorCodes('&',
                format == null ? "ETA: $timeLeft seconds, Speed: $placingSpeed block/sec, $percentage %" : format);
        //Swap in the variables
        format = format.replace("$jobsCount", jobsCount + "")
                .replace("$queuedBlocks", queuedBlocks + "")
                .replace("$maxQueuedBlocks", maxQueuedBlocks + "")
                .replace("$timeLeft", nf.format(smoothedTimeLeft))
                .replace("$placingSpeed", nf.format(placingSpeed))
                .replace("$percentage", nf.format(percentage));
        //Set the title
        bossBar.setTitle(format);
        //Show it to the player
        bossBar.setVisible(true);
        //Store it for later
        bossBars.put(player.getUUID(), bossBar);
    }
}

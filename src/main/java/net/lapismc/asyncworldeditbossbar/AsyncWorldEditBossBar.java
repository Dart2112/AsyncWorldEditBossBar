package net.lapismc.asyncworldeditbossbar;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.plugin.java.JavaPlugin;
import org.primesoft.asyncworldedit.AsyncWorldEditMain;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerEntry;
import org.primesoft.asyncworldedit.api.progressDisplay.IProgressDisplay;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public final class AsyncWorldEditBossBar extends JavaPlugin implements IProgressDisplay {

    private HashMap<UUID, BossBar> bossBars = new HashMap<>();

    @Override
    public void onEnable() {
        AsyncWorldEditMain.getInstance().getAPI().getProgressDisplayManager().registerProgressDisplay(this);
    }

    @Override
    public void onDisable() {
        AsyncWorldEditMain.getInstance().getAPI().getProgressDisplayManager().unregisterProgressDisplay(this);
    }

    @Override
    public void disableMessage(IPlayerEntry player) {
        if (!bossBars.containsKey(player.getUUID()))
            return;
        bossBars.get(player.getUUID()).removePlayer(Objects.requireNonNull(Bukkit.getPlayer(player.getUUID())));
        bossBars.remove(player.getUUID());
    }

    @Override
    public void setMessage(IPlayerEntry player, int jobsCount, int queuedBlocks, int maxQueuedBlocks, double timeLeft,
                           double placingSpeed, double percentage) {
        BossBar bossBar;
        if (bossBars.containsKey(player.getUUID())) {
            bossBar = bossBars.get(player.getUUID());
        } else {
            bossBar = Bukkit.createBossBar("World Edit", BarColor.BLUE, BarStyle.SOLID);
        }
        bossBar.setProgress(percentage / 100);
        NumberFormat nf = new DecimalFormat("#.##");
        bossBar.setTitle("ETA: " + nf.format(timeLeft) + " seconds, Speed: " + nf.format(placingSpeed) + " block/sec");
        bossBar.addPlayer(Objects.requireNonNull(Bukkit.getPlayer(player.getUUID())));
        bossBars.put(player.getUUID(), bossBar);
    }
}
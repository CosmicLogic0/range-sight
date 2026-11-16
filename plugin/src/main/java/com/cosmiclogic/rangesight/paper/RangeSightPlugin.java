package com.cosmiclogic.rangesight.paper;

import com.cosmiclogic.logiclib.paper.LogicText;
import com.cosmiclogic.logiclib.paper.LogicVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Range Sight for Paper: /range shows game ranges as particle outlines that only that player sees. */
public final class RangeSightPlugin extends JavaPlugin implements Listener {

    static final String PREFIX = "&8[&bRange Sight&8]";

    private final Map<UUID, Set<RangeType>> shown = new HashMap<>();
    private RangeDrawer drawer;

    @Override
    public void onEnable() {
        drawer = new RangeDrawer(this);
        getServer().getPluginManager().registerEvents(this, this);
        // Particles fade after about a second, so redraw every second.
        Bukkit.getScheduler().runTaskTimer(this, this::drawAll, 20L, 20L);
        getLogger().info("Range Sight " + getDescription().getVersion() + " enabled on Minecraft " + LogicVersion.current());
    }

    @Override
    public void onDisable() {
        shown.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        shown.remove(event.getPlayer().getUniqueId());
        drawer.forget(event.getPlayer().getUniqueId());
    }

    private void drawAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<RangeType> types = shown.get(player.getUniqueId());
            if (types != null && !types.isEmpty()) {
                drawer.draw(player, types);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("selftest")) {
                SelfTest.start(this, drawer, Bukkit.getWorlds().get(0), getLogger());
                return true;
            }
            LogicText.send(sender, PREFIX, "&cOnly players can use /range.");
            return true;
        }
        Player player = (Player) sender;
        Set<RangeType> types = shown.computeIfAbsent(player.getUniqueId(), id -> EnumSet.noneOf(RangeType.class));
        String arg = args.length == 0 ? "" : args[0].toLowerCase();

        if (arg.isEmpty() || arg.equals("all")) {
            if (!types.isEmpty() && arg.isEmpty()) {
                types.clear();
                LogicText.send(player, PREFIX, "&7Outlines &coff&7.");
            } else {
                types.addAll(EnumSet.allOf(RangeType.class));
                LogicText.send(player, PREFIX, "&7Showing &aall ranges&7. Type &f/range off &7to hide them.");
            }
            return true;
        }
        if (arg.equals("off")) {
            types.clear();
            LogicText.send(player, PREFIX, "&7Outlines &coff&7.");
            return true;
        }
        if (arg.equals("list")) {
            sendList(player, types);
            return true;
        }
        RangeType type = RangeType.byId(arg);
        if (type == null) {
            LogicText.send(player, PREFIX, "&cUnknown type &f" + arg + "&c. Use: " + typeIds() + ", all, off, list");
            return true;
        }
        if (types.remove(type)) {
            LogicText.send(player, PREFIX, "&7" + type.label + " &coff&7.");
        } else {
            types.add(type);
            LogicText.send(player, PREFIX, "&7" + type.label + " &aon&7.");
            if (type == RangeType.SPAWN_CHUNKS && LogicVersion.current().isAtLeast(1, 21, 9)) {
                LogicText.send(player, PREFIX, "&eThis Minecraft version has no spawn chunks (removed in 1.21.9).");
            }
        }
        return true;
    }

    private void sendList(Player player, Set<RangeType> types) {
        LogicText.send(player, PREFIX, "&7Range types (&aon&7/&8off&7):");
        for (RangeType type : RangeType.values()) {
            String hex = String.format("#%06X", type.color.asRGB());
            LogicText.send(player, (types.contains(type) ? "&a" : "&8") + " - " + type.id + " &7" + type.label + " &8(" + hex + ")");
        }
    }

    private static String typeIds() {
        StringBuilder out = new StringBuilder();
        for (RangeType type : RangeType.values()) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(type.id);
        }
        return out.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase();
            List<String> all = new ArrayList<>();
            for (RangeType type : RangeType.values()) {
                all.add(type.id);
            }
            all.add("all");
            all.add("off");
            all.add("list");
            for (String option : all) {
                if (option.startsWith(start)) {
                    out.add(option);
                }
            }
        }
        return out;
    }
}

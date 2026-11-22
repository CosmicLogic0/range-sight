package com.cosmiclogic.rangesight.paper;

import com.cosmiclogic.logiclib.paper.LogicConfigs;
import com.cosmiclogic.logiclib.paper.LogicParticles;
import com.cosmiclogic.logiclib.paper.LogicText;
import com.cosmiclogic.logiclib.paper.LogicVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
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
    private static final String[] WORDS = {"all", "off", "list", "help", "reload"};

    /** Each player's chosen ranges. Kept when they log out, until the server restarts. */
    private final Map<UUID, Set<RangeType>> shown = new HashMap<>();
    private RangeDrawer drawer;
    private PluginSettings settings = new PluginSettings();

    @Override
    public void onEnable() {
        if (!logicLibIsNewEnough()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        drawer = new RangeDrawer(this);
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
        // Particles fade after about a second, so redraw every second.
        Bukkit.getScheduler().runTaskTimer(this, this::drawAll, 20L, 20L);
        getLogger().info("Range Sight " + getDescription().getVersion() + " enabled on Minecraft " + LogicVersion.current()
                + " (long-distance particles " + (settings.farParticles && LogicParticles.farSupported() ? "on" : "off") + ")");
    }

    /** Range Sight 1.1.0 needs LogicLib Plugin 0.2.0+ (LogicParticles.FAR, LogicConfigs). Says so clearly if not. */
    private boolean logicLibIsNewEnough() {
        Plugin lib = getServer().getPluginManager().getPlugin("LogicLib");
        String have = lib == null ? null : lib.getDescription().getVersion();
        if (have != null && LibVersion.atLeast(have, LibVersion.NEEDED)) {
            return true;
        }
        getLogger().severe("Range Sight needs LogicLib Plugin " + LibVersion.NEEDED + " or newer, but this server has "
                + (have == null ? "no LogicLib" : "LogicLib " + have) + ". Download the latest LogicLib Plugin from "
                + "https://www.curseforge.com/minecraft/bukkit-plugins/logiclib-plugin and restart. Range Sight is turned off until then.");
        return false;
    }

    private void loadSettings() {
        settings = PluginSettings.from(LogicConfigs.load(this));
        for (String problem : settings.problems) {
            getLogger().warning(problem);
        }
        drawer.settings(settings);
    }

    @Override
    public void onDisable() {
        shown.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
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
        String arg = args.length == 0 ? "" : args[0].toLowerCase();
        if (arg.equals("help") || arg.equals("?")) {
            sendHelp(sender, label);
            return true;
        }
        if (arg.equals("reload")) {
            if (!sender.hasPermission("rangesight.admin")) {
                LogicText.send(sender, PREFIX, "&cYou need the permission &frangesight.admin &cto reload.");
                return true;
            }
            loadSettings();
            LogicText.send(sender, PREFIX, settings.problems.isEmpty() ? "&aSettings reloaded."
                    : "&eSettings reloaded with " + settings.problems.size() + " problem(s); see the server log.");
            return true;
        }
        if (!(sender instanceof Player)) {
            if (arg.equals("selftest")) {
                SelfTest.start(this, drawer, Bukkit.getWorlds().get(0), getLogger());
                return true;
            }
            LogicText.send(sender, PREFIX, "&cOnly players can show ranges. Try &f/" + label + " help&c.");
            return true;
        }
        Player player = (Player) sender;
        Set<RangeType> types = shown.computeIfAbsent(player.getUniqueId(), id -> EnumSet.noneOf(RangeType.class));

        if (arg.isEmpty() || arg.equals("all")) {
            if (!types.isEmpty() && arg.isEmpty()) {
                types.clear();
                LogicText.send(player, PREFIX, "&7Outlines &coff&7.");
            } else {
                types.addAll(EnumSet.allOf(RangeType.class));
                LogicText.send(player, PREFIX, "&7Showing &aall ranges&7. Type &f/" + label + " off &7to hide them, or &f/"
                        + label + " help&7.");
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
            LogicText.send(player, PREFIX, "&cUnknown range &f" + arg + "&c. Try &f/" + label + " help&c.");
            return true;
        }
        if (types.remove(type)) {
            LogicText.send(player, PREFIX, "&7" + type.label + " &coff&7.");
        } else {
            types.add(type);
            LogicText.send(player, PREFIX, "&7" + type.label + " &aon&7.");
            String missing = notOnThisVersion(type);
            if (missing != null) {
                LogicText.send(player, PREFIX, "&e" + missing);
            }
        }
        return true;
    }

    /** A note when a type can't exist on this Minecraft version, or null. */
    static String notOnThisVersion(RangeType type) {
        LogicVersion v = LogicVersion.current();
        switch (type) {
            case SPAWN_CHUNKS:
                return v.isAtLeast(1, 21, 9) ? "This Minecraft version has no spawn chunks (removed in 1.21.9)." : null;
            case TRIAL:
                return v.isAtLeast(1, 21, 0) ? null : "Trial spawners and vaults need Minecraft 1.21 or newer.";
            case SCULK:
                return v.isAtLeast(1, 19, 0) ? null : "Sculk blocks need Minecraft 1.19 or newer.";
            case RAID:
                return v.isAtLeast(1, 14, 0) ? null : "Raids need Minecraft 1.14 or newer.";
            case CONDUIT:
                return v.isAtLeast(1, 13, 0) ? null : "Conduits need Minecraft 1.13 or newer.";
            default:
                return null;
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        LogicText.send(sender, PREFIX, "&7Shows invisible game ranges as outlines that &fonly you &7see:");
        LogicText.send(sender, " &f/" + label + " &7- every range on, or off if some are on");
        LogicText.send(sender, " &f/" + label + " <type> &7- one range on/off:");
        for (RangeType type : RangeType.values()) {
            String hex = String.format("#%06X", type.color.asRGB());
            LogicText.send(sender, "   &f" + type.id + " &8(" + hex + ") &7" + type.help);
        }
        LogicText.send(sender, " &f/" + label + " all &7/ &f/" + label + " off &7- everything on / off");
        LogicText.send(sender, " &f/" + label + " list &7- what you have on");
        if (sender.hasPermission("rangesight.admin")) {
            LogicText.send(sender, " &f/" + label + " reload &7- reload config.yml (colors, scan distance)");
        }
    }

    private void sendList(Player player, Set<RangeType> types) {
        LogicText.send(player, PREFIX, "&7Range types (&aon&7/&8off&7):");
        for (RangeType type : RangeType.values()) {
            String hex = String.format("#%06X", type.color.asRGB());
            LogicText.send(player, (types.contains(type) ? "&a" : "&8") + " - " + type.id + " &7" + type.label + " &8(" + hex + ")");
        }
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
            for (String word : WORDS) {
                if (!word.equals("reload") || sender.hasPermission("rangesight.admin")) {
                    all.add(word);
                }
            }
            for (String option : all) {
                if (option.startsWith(start)) {
                    out.add(option);
                }
            }
        }
        return out;
    }
}

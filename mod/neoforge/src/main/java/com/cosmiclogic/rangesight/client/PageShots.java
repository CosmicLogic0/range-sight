package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeMath;
import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * CI only, never active for players. Takes the real in-game screenshots for the CurseForge gallery.
 * Turned on by config/rangesight-pageshots.txt, which only the CI screenshot jobs write:
 * "mod" makes the client test's new world Classic Flat (no structures), builds scenes with the integrated server
 * and shoots each with only the range type it is about switched on; "plugin host:port" joins a local Superflat
 * Paper server instead, builds the scenes with commands (the player is op there) and shows ranges with /range.
 * An empty see-through screen keeps the client test from quitting until every shot is saved.
 */
final class PageShots {

    private static String mode;
    private static String address;
    private static boolean holding;
    private static boolean connecting;
    private static int wait;
    private static int step;
    private static final List<Step> STEPS = new ArrayList<>();
    private static BlockPos base;

    private interface Step {
        /** Runs the step; returns how many ticks to wait before the next one. */
        int run(Minecraft mc);
    }

    private PageShots() {
    }

    static void init(Path configDir) {
        try {
            Path marker = configDir.resolve("rangesight-pageshots.txt");
            if (Files.exists(marker)) {
                String[] parts = new String(Files.readAllBytes(marker)).trim().split("\\s+");
                mode = parts[0];
                address = parts.length > 1 ? parts[1] : null;
                RangeSight.LOG.info("Range Sight page shots: mode {}", mode);
            }
        } catch (Exception e) {
            RangeSight.LOG.error("Range Sight page shots: bad marker", e);
        }
    }

    /** The client test opens "Create World": make it Classic Flat, or (plugin mode) join the Paper server instead. */
    static void onScreenOpening(ScreenEvent.Opening event) {
        if (mode == null || !(event.getNewScreen() instanceof CreateWorldScreen screen)) {
            return;
        }
        if ("plugin".equals(mode)) {
            event.setCanceled(true);
            connecting = true;
            return;
        }
        WorldCreationUiState ui = screen.getUiState();
        List<WorldCreationUiState.WorldTypeEntry> types = new ArrayList<>(ui.getNormalPresetList());
        types.addAll(ui.getAltPresetList());
        for (WorldCreationUiState.WorldTypeEntry type : types) {
            if (type.preset() != null && type.preset().is(WorldPresets.FLAT)) {
                ui.setWorldType(type);
                RangeSight.LOG.info("Range Sight page shots: new world is Superflat (Classic Flat)");
            }
        }
        ui.setGenerateStructures(false);
        ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        ui.setDifficulty(Difficulty.PEACEFUL);
    }

    static void tick() {
        if (mode == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (connecting) {
            connecting = false;
            ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
                    new ServerData("Range Sight shots", address, ServerData.Type.OTHER), false, null);
            RangeSight.LOG.info("Range Sight page shots: joining {}", address);
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!holding) {
            if (mc.screen != null) {
                return;
            }
            mc.setScreen(new HoldScreen());
            holding = true;
            setup(mc);
            RangeSight.LOG.info("Range Sight page shots: world shown, holding the client open");
            return;
        }
        if (step >= STEPS.size()) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        try {
            wait = STEPS.get(step++).run(mc);
        } catch (Throwable t) {
            RangeSight.LOG.error("Range Sight page shots: step " + step + " failed", t);
            wait = 2;
        }
    }

    // ------------------------------------------------------------------ setup

    private static void setup(Minecraft mc) {
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
        mc.options.graphicsMode().set(GraphicsStatus.FANCY);
        mc.options.renderDistance().set(16);
        mc.options.hideGui = true;
        base = mc.player.blockPosition();
        if ("plugin".equals(mode)) {
            pluginSteps();
        } else {
            modSteps();
        }
        STEPS.add(m -> {
            m.options.hideGui = false;
            if (m.screen != null) {
                m.setScreen(null);
            }
            RangeSight.LOG.info("Range Sight page shots: done");
            return 1;
        });
    }

    // ------------------------------------------------------------------ mod (single player, Superflat)

    private static void modSteps() {
        BlockPos p = base;
        BlockPos beacon = p.offset(0, 4, 26);
        BlockPos spawner = p.offset(-16, 0, 34);
        BlockPos sensor = p.offset(14, 0, 18);
        BlockPos calibrated = p.offset(19, 0, 30);
        BlockPos shrieker = p.offset(11, 0, 41);
        BlockPos tank = p.offset(260, 3, 26);
        BlockPos trial = p.offset(-40, 0, 6);

        STEPS.add(mc -> {
            server(mc, level -> {
                world(level);
                level.getServer().getPlayerList().setViewDistance(16);
                pyramid(level, beacon);
                level.setBlock(spawner, Blocks.SPAWNER.defaultBlockState(), 3);
                level.setBlock(sensor, Blocks.SCULK_SENSOR.defaultBlockState(), 3);
                level.setBlock(calibrated, Blocks.CALIBRATED_SCULK_SENSOR.defaultBlockState(), 3);
                level.setBlock(shrieker, Blocks.SCULK_SHRIEKER.defaultBlockState(), 3);
                conduitTank(level, tank);
                level.setBlock(trial, Blocks.TRIAL_SPAWNER.defaultBlockState(), 3);
                level.setBlock(trial.offset(6, 0, 0), Blocks.VAULT.defaultBlockState(), 3);
                ServerPlayer sp = player(mc);
                sp.setGameMode(GameType.CREATIVE);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
            });
            return 260; // let every chunk in view distance load, so no world edge shows
        });
        shot("all-ranges", EnumSet.of(RangeType.BEACON, RangeType.SPAWNER, RangeType.SCULK), beacon.offset(-40, 15, -46), beacon.offset(2, -4, 6));
        shot("beacon-outside", EnumSet.of(RangeType.BEACON), beacon.offset(-74, 16, -66), beacon.offset(0, 10, 0));
        shot("beacon-inside", EnumSet.of(RangeType.BEACON), beacon.offset(-20, -2, -15), beacon.offset(0, 1, 0));
        shot("spawner", EnumSet.of(RangeType.SPAWNER), spawner.offset(-14, 9, -22), spawner);
        shot("sculk", EnumSet.of(RangeType.SCULK), calibrated.offset(-46, 30, -40), calibrated.offset(-4, 0, 4));
        shot("spawn-chunks", EnumSet.of(RangeType.SPAWN_CHUNKS), p.offset(-46, 34, -60), p.offset(0, 6, 0));
        shot("trial", EnumSet.of(RangeType.TRIAL), trial.offset(-16, 9, -20), trial.offset(3, 0, 0));
        STEPS.add(mc -> {
            view(mc, tank.offset(-34, 12, -38), tank);
            return 120; // new chunks load
        });
        shot("conduit", EnumSet.of(RangeType.CONDUIT), tank.offset(-34, 12, -38), tank);
        // HUD: pressing a range key shows a message above the hotbar
        STEPS.add(mc -> {
            only(EnumSet.noneOf(RangeType.class));
            view(mc, beacon.offset(-30, -3, -30), beacon.offset(0, -1, 0));
            return 60;
        });
        STEPS.add(mc -> {
            mc.options.hideGui = false;
            KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(RangeType.BEACON.defaultKey));
            return 25;
        });
        STEPS.add(mc -> {
            RangeClient.rescanNow();
            return 6;
        });
        STEPS.add(mc -> save(mc, "mod-hud-toggle"));
        // The settings screen (numpad . or the mod list's Config button), over the world
        STEPS.add(mc -> {
            only(EnumSet.of(RangeType.BEACON, RangeType.SPAWNER, RangeType.SCULK, RangeType.TRIAL));
            view(mc, beacon.offset(-40, 15, -46), beacon.offset(2, -4, 6));
            mc.options.hideGui = false;
            mc.setScreen(new SettingsScreen(null));
            return 20;
        });
        STEPS.add(mc -> save(mc, "mod-settings"));
        // Controls: the Range Sight key binds
        STEPS.add(mc -> {
            mc.setScreen(keyBindsScreen(mc));
            return 10;
        });
        STEPS.add(mc -> {
            scrollKeyBinds(mc);
            return 10;
        });
        STEPS.add(mc -> save(mc, "mod-key-binds"));
    }

    private static void world(ServerLevel level) {
        level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.setDayTime(6000L);
        level.setWeatherParameters(20000, 0, false, false);
    }

    private static void pyramid(ServerLevel level, BlockPos beacon) {
        for (int l = 1; l <= 4; l++) {
            for (int dx = -l; dx <= l; dx++) {
                for (int dz = -l; dz <= l; dz++) {
                    level.setBlock(beacon.offset(dx, -l, dz), Blocks.IRON_BLOCK.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(beacon, Blocks.BEACON.defaultBlockState(), 3);
    }

    /** Glass tank of water with one full prismarine ring (16 frame blocks = 32-block range) and a conduit. */
    private static void conduitTank(ServerLevel level, BlockPos c) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    boolean shell = Math.abs(dx) == 3 || Math.abs(dy) == 3 || Math.abs(dz) == 3;
                    Block b = shell ? Blocks.GLASS : Blocks.WATER;
                    level.setBlock(c.offset(dx, dy, dz), b.defaultBlockState(), 2);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (RangeMath.isConduitFramePosition(dx, 0, dz)) {
                    level.setBlock(c.offset(dx, 0, dz), Blocks.PRISMARINE_BRICKS.defaultBlockState(), 2);
                }
            }
        }
        level.setBlock(c, Blocks.CONDUIT.defaultBlockState(), 3);
    }

    // ------------------------------------------------------------------ plugin (Paper server, /range)

    private static void pluginSteps() {
        BlockPos p = base;
        BlockPos beacon = p.offset(0, 4, 26);
        BlockPos spawner = p.offset(-16, 0, 34);
        BlockPos sensor = p.offset(14, 0, 18);
        BlockPos calibrated = p.offset(19, 0, 30);
        BlockPos shrieker = p.offset(11, 0, 41);
        BlockPos tank = p.offset(40, 3, -30);
        BlockPos trial = p.offset(-40, 0, 6);

        List<String> build = new ArrayList<>();
        build.add("time set 6000");
        build.add("weather clear");
        build.add("gamerule doDaylightCycle false");
        build.add("gamerule doWeatherCycle false");
        build.add("gamerule doMobSpawning false");
        build.add("gamemode creative");
        build.add("gamerule sendCommandFeedback false");
        for (int l = 1; l <= 4; l++) {
            build.add(fill(beacon.offset(-l, -l, -l), beacon.offset(l, -l, l), "iron_block"));
        }
        build.add(set(beacon, "beacon"));
        build.add(set(spawner, "spawner"));
        build.add(set(sensor, "sculk_sensor"));
        build.add(set(calibrated, "calibrated_sculk_sensor"));
        build.add(set(shrieker, "sculk_shrieker"));
        build.add(fill(tank.offset(-3, -3, -3), tank.offset(3, 3, 3), "glass"));
        build.add(fill(tank.offset(-2, -2, -2), tank.offset(2, 2, 2), "water"));
        build.add(fill(tank.offset(-2, 0, -2), tank.offset(2, 0, 2), "prismarine_bricks"));
        build.add(fill(tank.offset(-1, 0, -1), tank.offset(1, 0, 1), "water"));
        build.add(set(tank, "conduit"));
        build.add(set(trial, "trial_spawner"));
        build.add(set(trial.offset(6, 0, 0), "vault"));
        STEPS.add(mc -> {
            for (String c : build) {
                command(mc, c);
            }
            return 200;
        });
        pshot("all-ranges", "all", beacon.offset(-12, 7, -8), beacon.offset(-2, -4, 10));
        pshot("beacon", "beacon", beacon.offset(-58, 6, -26), beacon.offset(-50, 2, 8));
        pshot("spawner", "spawner", spawner.offset(-11, 5, -13), spawner.offset(0, 1, 0));
        pshot("sculk", "sculk", calibrated.offset(-14, 6, -12), calibrated.offset(-2, 0, 2));
        pshot("conduit", "conduit", tank.offset(-18, 6, -20), tank);
        pshot("spawn-chunks", "spawnchunks", p.offset(-30, 6, -36), p.offset(-20, 2, -8));
        pshot("particles-close", "spawner", spawner.offset(-8, 4, -10), spawner.offset(0, 3, 0));
        pshot("trial", "trial", trial.offset(-16, 7, -20), trial.offset(3, 0, 0));
        // Long-distance particles (LogicLib 0.2.0): the whole 50-block beacon range from far outside it
        pshot("beacon-far", "beacon", beacon.offset(-78, 22, -70), beacon.offset(0, 4, 0));
        // chat close-ups with a large GUI so the text fills the picture
        STEPS.add(mc -> {
            command(mc, "range off");
            view(mc, beacon.offset(-30, -3, -30), beacon.offset(0, -1, 0));
            mc.options.guiScale().set(4);
            mc.resizeDisplay();
            return 30;
        });
        STEPS.add(mc -> {
            mc.options.hideGui = false;
            command(mc, "range beacon");
            command(mc, "range spawner");
            command(mc, "range list");
            return 30;
        });
        STEPS.add(mc -> save(mc, "plugin-chat-list"));
        STEPS.add(mc -> {
            command(mc, "range off");
            command(mc, "range all");
            return 30;
        });
        STEPS.add(mc -> save(mc, "plugin-chat-all"));
    }

    private static void pshot(String name, String rangeArg, BlockPos eye, BlockPos target) {
        STEPS.add(mc -> {
            mc.options.hideGui = true;
            command(mc, "range off");
            command(mc, "range " + rangeArg);
            view(mc, eye, target);
            return 70;
        });
        STEPS.add(mc -> save(mc, "plugin-" + name));
    }

    private static String set(BlockPos b, String block) {
        return "setblock " + b.getX() + " " + b.getY() + " " + b.getZ() + " " + block;
    }

    private static String fill(BlockPos a, BlockPos b, String block) {
        return "fill " + a.getX() + " " + a.getY() + " " + a.getZ() + " " + b.getX() + " " + b.getY() + " " + b.getZ() + " " + block;
    }

    private static void command(Minecraft mc, String c) {
        mc.player.connection.sendCommand(c);
    }

    // ------------------------------------------------------------------ shared helpers

    private static void shot(String name, Set<RangeType> types, BlockPos eye, BlockPos target) {
        STEPS.add(mc -> {
            only(types);
            mc.options.hideGui = true;
            view(mc, eye, target);
            return 50;
        });
        STEPS.add(mc -> {
            RangeClient.rescanNow();
            return 8;
        });
        STEPS.add(mc -> save(mc, "mod-" + name));
    }

    private static void only(Set<RangeType> types) {
        RangeConfig config = RangeClient.config();
        for (RangeType t : RangeType.values()) {
            config.setEnabled(t, types.contains(t));
        }
        RangeClient.rescanNow();
    }

    /** Puts the camera at eye, looking at target (server-side teleport in single player, /tp on Paper). */
    private static void view(Minecraft mc, BlockPos eye, BlockPos target) {
        double x = eye.getX() + 0.5, y = eye.getY(), z = eye.getZ() + 0.5;
        double tx = target.getX() + 0.5 - x, ty = target.getY() + 0.5 - (y + 1.62), tz = target.getZ() + 0.5 - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty, Math.sqrt(tx * tx + tz * tz)));
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                sp.teleportTo(server.overworld(), x, y, z, yaw, pitch);
            });
        } else {
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
            command(mc, String.format(Locale.ROOT, "tp @s %.1f %.1f %.1f %.1f %.1f", x, y, z, yaw, pitch));
        }
    }

    private static int save(Minecraft mc, String name) {
        int w = mc.getMainRenderTarget().width, h = mc.getMainRenderTarget().height;
        Screenshot.grab(mc.gameDirectory, "rangesight-" + name + ".png", mc.getMainRenderTarget(),
                msg -> RangeSight.LOG.info("Range Sight page shot saved: {} ({}x{})", msg.getString(), w, h));
        return 6;
    }

    private static void server(Minecraft mc, Consumer<ServerLevel> work) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> work.accept(server.overworld()));
    }

    private static ServerPlayer player(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private static Screen keyBindsScreen(Minecraft mc) {
        for (String cls : new String[]{"net.minecraft.client.gui.screens.options.controls.KeyBindsScreen",
                "net.minecraft.client.gui.screens.controls.KeyBindsScreen"}) {
            try {
                return (Screen) Class.forName(cls).getConstructor(Screen.class, net.minecraft.client.Options.class)
                        .newInstance(mc.screen, mc.options);
            } catch (ReflectiveOperationException ignored) {
                // try the next name
            }
        }
        throw new IllegalStateException("KeyBindsScreen not found");
    }

    /** Scrolls the key binds list so the Range Sight category is near the top. */
    private static void scrollKeyBinds(Minecraft mc) {
        Screen screen = mc.screen;
        if (screen == null) {
            return;
        }
        for (Object child : screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractSelectionList<?> list) {
                int index = 0;
                for (Object entry : list.children()) {
                    String text = entry.toString();
                    try {
                        for (java.lang.reflect.Field f : entry.getClass().getDeclaredFields()) {
                            if (Component.class.isAssignableFrom(f.getType())) {
                                f.setAccessible(true);
                                Object v = f.get(entry);
                                if (v != null) {
                                    text = text + " " + ((Component) v).getString();
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // keep toString
                    }
                    if (text.contains("Range Sight")) {
                        list.setScrollAmount(Math.max(0, index - 1) * 20.0);
                        RangeSight.LOG.info("Range Sight page shots: key binds scrolled to entry {}", index);
                        return;
                    }
                    index++;
                }
                RangeSight.LOG.info("Range Sight page shots: Range Sight category not found in {} entries", index);
            }
        }
    }

    /** Draws nothing, so the world shows through; doesn't pause the game. */
    private static final class HoldScreen extends Screen {
        HoldScreen() {
            super(Component.empty());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }
    }
}

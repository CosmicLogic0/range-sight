package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeMath;
import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Range Sight on Minecraft 1.12.2: keys, scanning once a second, drawing every frame, and the CI self-test.
 * 1.12.2 has beacons, spawners and spawn chunks (conduits came in 1.13, raids in 1.14, sculk in 1.17).
 */
public final class RangeClient12 {

    private static final int SCAN_EVERY_TICKS = 20;
    private static final int MAX_SHAPES = 48;
    private static final int[] MC_VERSION = {1, 12, 2};

    private static RangeConfig config;
    private static final Map<RangeType, KeyBinding> TYPE_KEYS = new EnumMap<>(RangeType.class);
    private static KeyBinding allKey;
    private static volatile List<RangeShape> shapes = Collections.emptyList();
    private static int ticks = SCAN_EVERY_TICKS;
    /** The world the outlines belong to; a new world or dimension clears them right away. */
    private static Object shapesLevel;
    private static boolean loggedFirstDraw;
    private static boolean loggedFirstFrame;
    static final int[] VERSION = {1, 12, 2};
    private static int selfTestStage;
    private static int selfTestTicks;

    private RangeClient12() {
    }

    public static void preInit(Path configDir) {
        config = new RangeConfig(configDir);
        config.load();
        // Only ranges 1.12.2 has (no conduits, raids, sculk or trial spawners yet) get a key.
        for (RangeType type : RangeType.availableTypes(VERSION)) {
            TYPE_KEYS.put(type, new KeyBinding(type.keyName(), lwjgl2Key(type.defaultKey), RangeType.KEY_CATEGORY));
        }
        allKey = new KeyBinding(RangeType.KEY_ALL, lwjgl2Key(RangeType.KEY_ALL_DEFAULT), RangeType.KEY_CATEGORY);
    }

    public static void init() {
        for (KeyBinding k : TYPE_KEYS.values()) {
            ClientRegistry.registerKeyBinding(k);
        }
        ClientRegistry.registerKeyBinding(allKey);
        MinecraftForge.EVENT_BUS.register(new Events());
        RangeSight.LOG.info("Range Sight client ready");
    }

    /** RangeType keeps GLFW key numbers (numpad 0-6); 1.12.2 uses LWJGL 2 key codes. */
    private static int lwjgl2Key(int glfw) {
        switch (glfw) {
            case 320: return 82;  // numpad 0
            case 321: return 79;  // numpad 1
            case 322: return 80;
            case 323: return 81;
            case 324: return 75;
            case 325: return 76;
            case 326: return 77;  // numpad 6
            case 327: return 71;  // numpad 7
            case 330: return 83;  // numpad . (decimal)
            default: return 0;
        }
    }

    public static final class Events {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                tick();
            }
        }

        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent event) {
            render(event.getPartialTicks());
        }
    }

    // ---------------------------------------------------------------- tick
    private static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean changed = false;
        for (Map.Entry<RangeType, KeyBinding> e : TYPE_KEYS.entrySet()) {
            while (e.getValue().isPressed()) {
                boolean on = !config.isEnabled(e.getKey());
                config.setEnabled(e.getKey(), on);
                say(mc, e.getKey().labelKey(), on);
                changed = true;
            }
        }
        while (allKey.isPressed()) {
            boolean on = !config.anyEnabled();
            config.setAll(on);
            say(mc, RangeSight.MOD_ID + ".all", on);
            changed = true;
        }
        if (mc.world == null || mc.player == null) {
            shapes = Collections.emptyList();
            ticks = SCAN_EVERY_TICKS;
            return;
        }
        if (mc.world != shapesLevel) {
            shapesLevel = mc.world;
            shapes = Collections.emptyList(); // never draw the old dimension's outlines in the new one
            ticks = SCAN_EVERY_TICKS;
        }
        showHints(mc);
        selfTest(mc);
        if (changed || ++ticks >= SCAN_EVERY_TICKS) {
            ticks = 0;
            shapes = config.anyEnabled() ? scan(mc) : Collections.<RangeShape>emptyList();
            if (!loggedFirstDraw && !shapes.isEmpty()) {
                loggedFirstDraw = true;
                RangeSight.LOG.info("Range Sight drawing " + shapes.size() + " outlines");
            }
        }
    }

    /** Once per install: how to use Range Sight. Once per broken config: where the old file went. */
    private static void showHints(Minecraft mc) {
        if (!config.hintShown && mc.player.ticksExisted > 40) {
            config.hintShown = true;
            config.save();
            chat(mc, RangeSight.MOD_ID + ".hint", allKey.getDisplayName(), false);
        }
        if (config.brokenFileNote != null && mc.player.ticksExisted > 40) {
            chat(mc, RangeSight.MOD_ID + ".broken_config", config.brokenFileNote, true);
            config.brokenFileNote = null;
        }
    }

    private static void chat(Minecraft mc, String key, String arg, boolean warning) {
        TextComponentString value = new TextComponentString(arg);
        value.setStyle(new Style().setColor(warning ? TextFormatting.WHITE : TextFormatting.YELLOW));
        TextComponentTranslation msg = new TextComponentTranslation(key, value);
        msg.setStyle(new Style().setColor(warning ? TextFormatting.GOLD : TextFormatting.GRAY));
        mc.player.sendMessage(msg);
    }

    private static void say(Minecraft mc, String key, boolean on) {
        if (mc.player == null) {
            return;
        }
        TextComponentTranslation msg = new TextComponentTranslation(key);
        msg.setStyle(new Style().setColor(TextFormatting.AQUA));
        TextComponentTranslation state = new TextComponentTranslation(on ? "options.on" : "options.off");
        state.setStyle(new Style().setColor(on ? TextFormatting.GREEN : TextFormatting.RED));
        mc.player.sendStatusMessage(msg.appendSibling(new TextComponentString(": ")).appendSibling(state), true);
    }

    // ---------------------------------------------------------------- scan
    private static List<RangeShape> scan(Minecraft mc) {
        List<RangeShape> out = new ArrayList<>();
        WorldClient world = mc.world;
        final double px = mc.player.posX;
        final double py = mc.player.posY;
        final double pz = mc.player.posZ;
        int pcx = MathHelper.floor(px) >> 4;
        int pcz = MathHelper.floor(pz) >> 4;
        int r = config.scanChunks;
        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }
                for (TileEntity te : chunk.getTileEntityMap().values()) {
                    addTileEntity(world, te, out);
                }
            }
        }
        if (config.isEnabled(RangeType.SPAWN_CHUNKS) && world.provider.getDimension() == 0) {
            BlockPos spawn = world.getSpawnPoint();
            int[] c = RangeMath.spawnChunks(MC_VERSION, spawn.getX(), spawn.getZ(), 2);
            if (c != null) {
                out.add(RangeShape.column(RangeType.SPAWN_CHUNKS, config.color(RangeType.SPAWN_CHUNKS),
                        c[0] * 16, c[1] * 16, (c[2] + 1) * 16, (c[3] + 1) * 16, 0, world.getHeight()));
            }
        }
        out.sort(Comparator.comparingDouble(s -> s.distanceSq(px, py, pz)));
        return out.size() > MAX_SHAPES ? new ArrayList<>(out.subList(0, MAX_SHAPES)) : out;
    }

    private static void addTileEntity(WorldClient world, TileEntity te, List<RangeShape> out) {
        BlockPos p = te.getPos();
        double x = p.getX() + 0.5;
        double y = p.getY() + 0.5;
        double z = p.getZ() + 0.5;
        if (te instanceof TileEntityBeacon && config.isEnabled(RangeType.BEACON)) {
            int range = RangeMath.beaconRange(beaconLevels(world, p));
            if (range > 0) {
                out.add(RangeShape.column(RangeType.BEACON, config.color(RangeType.BEACON),
                        p.getX() - range, p.getZ() - range, p.getX() + 1 + range, p.getZ() + 1 + range,
                        p.getY() - range, world.getHeight()));
            }
        } else if (te instanceof TileEntityMobSpawner && config.isEnabled(RangeType.SPAWNER)) {
            int playerRange = RangeMath.SPAWNER_DEFAULT_PLAYER_RANGE;
            int spawnRange = RangeMath.SPAWNER_DEFAULT_SPAWN_RANGE;
            try {
                NBTTagCompound tag = ((TileEntityMobSpawner) te).getSpawnerBaseLogic().writeToNBT(new NBTTagCompound());
                if (tag.hasKey("RequiredPlayerRange")) {
                    playerRange = tag.getShort("RequiredPlayerRange");
                }
                if (tag.hasKey("SpawnRange")) {
                    spawnRange = tag.getShort("SpawnRange");
                }
            } catch (RuntimeException ignored) {
                // keep the defaults
            }
            int color = config.color(RangeType.SPAWNER);
            out.add(RangeShape.sphere(RangeType.SPAWNER, color, x, y, z, playerRange));
            out.add(RangeShape.box(RangeType.SPAWNER, color, x - spawnRange, p.getY() - 1, z - spawnRange,
                    x + spawnRange, p.getY() + 2, z + spawnRange));
        }
    }

    private static int beaconLevels(WorldClient world, BlockPos beacon) {
        int levels = 0;
        for (int l = 1; l <= 4; l++) {
            int y = beacon.getY() - l;
            if (y < 0) {
                break;
            }
            for (int x = beacon.getX() - l; x <= beacon.getX() + l; x++) {
                for (int z = beacon.getZ() - l; z <= beacon.getZ() + l; z++) {
                    BlockPos at = new BlockPos(x, y, z);
                    IBlockState state = world.getBlockState(at);
                    if (!state.getBlock().isBeaconBase(world, at, beacon)) {
                        return levels;
                    }
                }
            }
            levels = l;
        }
        return levels;
    }

    // ---------------------------------------------------------------- draw
    private static void render(float partialTicks) {
        List<RangeShape> current = shapes;
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (current.isEmpty() || view == null || mc.player == null) {
            return;
        }
        double cx = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double cy = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double cz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
        double ringY = mc.player.posY + 0.05;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(config.lineWidth);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (RangeShape shape : current) {
            emit(buf, shape.segments, cx, cy, cz, shape.color);
            emit(buf, shape.playerRing(ringY), cx, cy, cz, shape.color);
        }
        tess.draw();
        GlStateManager.glLineWidth(1.0f);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        if (!loggedFirstFrame) {
            loggedFirstFrame = true;
            RangeSight.LOG.info("Range Sight rendered its first frame of outlines");
        }
    }

    private static void emit(BufferBuilder buf, double[] seg, double cx, double cy, double cz, int argb) {
        int r = argb >> 16 & 255;
        int g = argb >> 8 & 255;
        int b = argb & 255;
        for (int i = 0; i + 5 < seg.length; i += 6) {
            buf.pos(seg[i] - cx, seg[i + 1] - cy, seg[i + 2] - cz).color(r, g, b, 255).endVertex();
            buf.pos(seg[i + 3] - cx, seg[i + 4] - cy, seg[i + 5] - cz).color(r, g, b, 255).endVertex();
        }
    }

    // ---------------------------------------------------------------- CI self-test
    /** Only with config/rangesight-selftest.txt (CI): builds a beacon pyramid in single player and checks it is found. */
    private static void selfTest(Minecraft mc) {
        if (!config.selfTest || selfTestStage >= 2) {
            return;
        }
        final IntegratedServer server = mc.getIntegratedServer();
        if (server == null) {
            return;
        }
        if (selfTestStage == 0) {
            if (++selfTestTicks < 40) {
                return;
            }
            config.setAll(true); // the test may start from a broken (reset) config with every range off
            final BlockPos beacon = new BlockPos(MathHelper.floor(mc.player.posX) + 3, MathHelper.floor(mc.player.posY) + 7, MathHelper.floor(mc.player.posZ) + 3);
            final int dim = mc.world.provider.getDimension();
            server.addScheduledTask(() -> {
                WorldServer w = server.getWorld(dim);
                for (int l = 1; l <= 4; l++) {
                    for (int dx = -l; dx <= l; dx++) {
                        for (int dz = -l; dz <= l; dz++) {
                            w.setBlockState(beacon.add(dx, -l, dz), Blocks.IRON_BLOCK.getDefaultState(), 3);
                        }
                    }
                }
                w.setBlockState(beacon, Blocks.BEACON.getDefaultState(), 3);
            });
            RangeSight.LOG.info("Range Sight client self-test: built a beacon pyramid at {}", beacon);
            selfTestStage = 1;
            selfTestTicks = 0;
        } else if (++selfTestTicks >= 30) {
            shapes = scan(mc);
            boolean found = false;
            for (RangeShape s : shapes) {
                if (s.type == RangeType.BEACON && Math.round(s.c - s.a) == 101) {
                    found = true;
                }
            }
            if (found) {
                RangeSight.LOG.info("Range Sight client self-test PASS: found the beacon's 50-block range ({} outlines)", shapes.size());
            } else {
                RangeSight.LOG.error("Range Sight client self-test FAIL: beacon range not found ({} outlines)", shapes.size());
            }
            selfTestStage = 2;
        }
    }
}

package com.cosmiclogic.rangesight.client;

import com.cosmiclogic.rangesight.RangeSight;
import com.cosmiclogic.rangesight.RangeType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * The Range Sight settings screen for Minecraft 1.12.2: the same rows as on newer versions (each range on/off and
 * its color, All on/off, line width, search distance). Opened with its key or the mod list's "Config" button.
 */
public final class SettingsScreen12 extends GuiScreen {

    private static final int ROW = 22;
    private final GuiScreen parent;
    private final List<Runnable> actions = new ArrayList<>();
    private final List<Runnable> labels = new ArrayList<>();
    private final List<String> tips = new ArrayList<>();
    private int top;
    private int rows;

    public SettingsScreen12(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        actions.clear();
        labels.clear();
        tips.clear();
        RangeConfig config = RangeClient12.config();
        String id = RangeSight.MOD_ID;
        List<RangeType> types = RangeType.availableTypes(RangeClient12.VERSION);
        rows = (types.size() + 1) / 2 + 3;
        top = Math.max(34, (height - rows * ROW) / 2);
        for (int i = 0; i < types.size(); i++) {
            RangeType type = types.get(i);
            int x = width / 2 + (i % 2 == 0 ? -205 : 5);
            int y = top + (i / 2) * ROW;
            GuiButton toggle = add(x, y, 150, id + ".help." + type.id, () -> config.setEnabled(type, !config.isEnabled(type)));
            labels.add(() -> {
                toggle.displayString = I18n.format(id + (config.isEnabled(type) ? ".settings.on" : ".settings.off"),
                        I18n.format(type.labelKey()));
                toggle.packedFGColour = config.isEnabled(type) ? 0 : 0xA0A0A0;
            });
            GuiButton color = add(x + 152, y, 48, id + ".settings.color.tip", () -> config.nextColor(type));
            labels.add(() -> {
                color.displayString = I18n.format(id + ".settings.color");
                color.packedFGColour = config.color(type) & 0xFFFFFF;
            });
        }
        int y = top + ((types.size() + 1) / 2) * ROW;
        GuiButton allOn = add(width / 2 - 205, y, 98, null, () -> config.setAll(true));
        GuiButton allOff = add(width / 2 - 103, y, 98, null, () -> config.setAll(false));
        GuiButton widthButton = add(width / 2 + 5, y, 200, id + ".settings.width.tip", config::nextLineWidth);
        GuiButton distance = add(width / 2 - 205, y + ROW, 410, id + ".settings.distance.tip", config::nextScanChunks);
        GuiButton done = add(width / 2 - 100, y + 2 * ROW, 200, null, () -> mc.displayGuiScreen(parent));
        labels.add(() -> {
            allOn.displayString = I18n.format(id + ".settings.all_on");
            allOff.displayString = I18n.format(id + ".settings.all_off");
            widthButton.displayString = I18n.format(id + ".settings.width", Math.round(config.lineWidth));
            distance.displayString = I18n.format(id + ".settings.distance", config.scanChunks);
            done.displayString = I18n.format("gui.done");
        });
        refresh();
    }

    private GuiButton add(int x, int y, int w, String tip, Runnable action) {
        GuiButton b = new GuiButton(buttonList.size(), x, y, w, 20, "");
        buttonList.add(b);
        actions.add(action);
        tips.add(tip);
        return b;
    }

    private void refresh() {
        for (Runnable r : labels) {
            r.run();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id >= 0 && button.id < actions.size()) {
            actions.get(button.id).run();
            RangeClient12.rescanSoon();
            if (mc.currentScreen == this) {
                refresh();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format(RangeSight.MOD_ID + ".settings.title"), width / 2, top - 22, 0xFFFFFF);
        drawCenteredString(fontRenderer, I18n.format(RangeSight.MOD_ID + ".settings.keys"), width / 2, top + rows * ROW + 4, 0xA0A0A0);
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (GuiButton b : buttonList) {
            String tip = tips.get(b.id);
            if (tip != null && b.isMouseOver()) {
                drawHoveringText(fontRenderer.listFormattedStringToWidth(I18n.format(tip), 220), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

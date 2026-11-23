package com.cosmiclogic.rangesight.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Range Sight settings screen (Minecraft 1.20 and newer): turn each range on or off, pick its color, line width
 * and search distance. Opened with its key (numpad . by default) or from the loader's mod list. Minecraft 1.16.5 to
 * 1.19.2 get their own copy with the older button classes.
 */
public final class SettingsScreen extends Screen {

    private final Screen parent;
    private final List<Button> buttons = new ArrayList<>();
    private final List<SettingsRows.Spec> specs = new ArrayList<>();

    public SettingsScreen(Screen parent) {
        super(Component.literal(SettingsRows.title()));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buttons.clear();
        specs.clear();
        RangeConfig config = RangeClient.config();
        int rows = SettingsRows.rows(RangeClient.mcVersion());
        int top = Math.max(34, (height - rows * SettingsRows.ROW) / 2);
        addRenderableWidget(new StringWidget(0, top - 22, width, 9, title, font));
        for (SettingsRows.Spec spec : SettingsRows.build(config, RangeClient.mcVersion(), this::onClose)) {
            Button.Builder b = Button.builder(text(spec), btn -> {
                spec.press.run();
                RangeClient.rescanSoon();
                refresh();
            }).bounds(width / 2 + spec.x, top + spec.row * SettingsRows.ROW, spec.width, 20);
            String tip = SettingsRows.tip(spec);
            if (tip != null) {
                b.tooltip(Tooltip.create(Component.literal(tip)));
            }
            buttons.add(addRenderableWidget(b.build()));
            specs.add(spec);
        }
        addRenderableWidget(new StringWidget(0, top + rows * SettingsRows.ROW + 4, width, 9,
                Component.literal(SettingsRows.keysLine()), font));
    }

    private void refresh() {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setMessage(text(specs.get(i)));
        }
    }

    private static Component text(SettingsRows.Spec spec) {
        MutableComponent c = Component.literal(spec.label());
        int color = spec.color();
        return color < 0 ? c : c.withStyle(s -> s.withColor(TextColor.fromRgb(color)));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}

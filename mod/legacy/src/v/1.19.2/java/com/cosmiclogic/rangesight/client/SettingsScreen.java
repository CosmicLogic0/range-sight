package com.cosmiclogic.rangesight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import java.util.ArrayList;
import java.util.List;

/** The Range Sight settings screen for Minecraft 1.19.2 (older button classes; same rows as on newer versions). */
public final class SettingsScreen extends Screen {

    private final Screen parent;
    private final List<Button> buttons = new ArrayList<>();
    private final List<SettingsRows.Spec> specs = new ArrayList<>();
    private int top;
    private int rows;

    public SettingsScreen(Screen parent) {
        super(Component.literal(SettingsRows.title()));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buttons.clear();
        specs.clear();
        RangeConfig config = RangeClient.config();
        rows = SettingsRows.rows(RangeClient.mcVersion());
        top = Math.max(34, (height - rows * SettingsRows.ROW) / 2);
        for (SettingsRows.Spec spec : SettingsRows.build(config, RangeClient.mcVersion(), this::onClose)) {
            String tip = SettingsRows.tip(spec);
            Button.OnPress press = btn -> {
                spec.press.run();
                RangeClient.rescanSoon();
                refresh();
            };
            int x = width / 2 + spec.x;
            int y = top + spec.row * SettingsRows.ROW;
            Button button = tip == null
                    ? new Button(x, y, spec.width, 20, text(spec), press)
                    : new Button(x, y, spec.width, 20, text(spec), press,
                    (btn, pose, mouseX, mouseY) -> renderTooltip(pose, font.split(Component.literal(tip), 220), mouseX, mouseY));
            addRenderableWidget(button);
            buttons.add(button);
            specs.add(spec);
        }
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
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        drawCenteredString(pose, font, title, width / 2, top - 22, 0xFFFFFF);
        drawCenteredString(pose, font, Component.literal(SettingsRows.keysLine()), width / 2, top + rows * SettingsRows.ROW + 4, 0xA0A0A0);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}

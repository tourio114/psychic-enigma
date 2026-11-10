package dev.island;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Live glass preview; slider changes remain in memory until leaving this screen. */
public final class AppearanceScreen extends Screen {
    private final Screen parent;
    private String error = "";
    public AppearanceScreen(Screen parent) { super(Component.literal("灵动岛外观")); this.parent = parent; }
    @Override protected void init() {
        int x = width / 2 - 140;
        addRenderableWidget(new AbstractSliderButton(x, 86, 280, 20, Component.empty(), IslandClient.CONFIG.opacity) {
            { updateMessage(); }
            @Override protected void updateMessage() {
                setMessage(Component.literal("背景不透明度：" + Math.round(value * 100) + "%"));
            }
            @Override protected void applyValue() { IslandClient.CONFIG.opacity = (float) value; }
        });
        addRenderableWidget(Button.builder(glassLabel(), b -> {
            IslandClient.CONFIG.glass = !IslandClient.CONFIG.glass; b.setMessage(glassLabel());
        }).bounds(x, 112, 136, 20).build());
        addRenderableWidget(Button.builder(animationLabel(), b -> {
            IslandClient.CONFIG.animations = !IslandClient.CONFIG.animations; b.setMessage(animationLabel());
        }).bounds(x + 144, 112, 136, 20).build());
        addRenderableWidget(Button.builder(Component.literal("恢复默认外观"), b -> {
            IslandClient.CONFIG.opacity = .62f;
            IslandClient.CONFIG.glass = IslandClient.CONFIG.animations = true;
            rebuildWidgets();
        }).bounds(x, 144, 280, 20).build());
        addRenderableWidget(Button.builder(Component.literal("保存并返回"), b -> onClose())
                .bounds(x, 176, 280, 20).build());
    }
    private Component glassLabel() { return Component.literal("玻璃质感：" + (IslandClient.CONFIG.glass ? "开" : "关")); }
    private Component animationLabel() { return Component.literal("动画：" + (IslandClient.CONFIG.animations ? "开" : "关")); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        // Use an explicit preview position, without writing temporary coordinates to config.
        IslandClient.drawPreview(g);
        g.centeredText(font, "灵动岛 · 外观预览", width / 2, 49, 0xFFFFFFFF);
        g.centeredText(font, "半透明渐变、高光与细颗粒；不包含背景模糊", width / 2, 68, 0xFFCED2E2);
        if (!error.isEmpty()) g.centeredText(font, error, width / 2, height - 20, 0xFFFF9999);
    }
    @Override public void onClose() {
        try { IslandClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
        catch (Exception e) { error = "保存失败，请检查配置目录权限"; }
    }
    @Override public boolean isPauseScreen() { return false; }
}

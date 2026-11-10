package dev.island;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Live glass preview; slider changes remain in memory until leaving this screen. */
public final class AppearanceScreen extends IslandUi.CardScreen {
    private final Screen parent;
    private String error = "";
    public AppearanceScreen(Screen parent) { super("外观与动画", "LIQUID GLASS  /  实时预览"); this.parent = parent; }
    @Override protected void init() {
        layout();
        int x = left + 14, full = cardWidth - 28, half = (full - 8) / 2;
        addRenderableWidget(new IslandUi.Slider(x, top + 100, full, IslandClient.CONFIG.opacity) {
            { updateMessage(); }
            @Override protected void updateMessage() {
                setMessage(Component.literal("背景不透明度：" + Math.round(value * 100) + "%"));
            }
            @Override protected void applyValue() { IslandClient.CONFIG.opacity = (float) value; }
        });
        action(glassLabel().getString(), x, top + 128, half, false, () -> {
            IslandClient.CONFIG.glass = !IslandClient.CONFIG.glass; rebuildWidgets();
        });
        action(animationLabel().getString(), x + half + 8, top + 128, full - half - 8, false, () -> {
            IslandClient.CONFIG.animations = !IslandClient.CONFIG.animations; rebuildWidgets();
        });
        action("恢复默认", x, top + 158, half, false, () -> {
            IslandClient.CONFIG.opacity = .62f;
            IslandClient.CONFIG.glass = IslandClient.CONFIG.animations = true; rebuildWidgets();
        });
        action("保存并返回", x + half + 8, top + 158, full - half - 8, true, this::onClose);
    }
    private Component glassLabel() { return Component.literal("玻璃质感：" + (IslandClient.CONFIG.glass ? "开" : "关")); }
    private Component animationLabel() { return Component.literal("动画：" + (IslandClient.CONFIG.animations ? "开" : "关")); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        // Use an explicit preview position, without writing temporary coordinates to config.
        IslandClient.drawPreviewAt(g, top + 55);
        if (cardHeight >= 250) label(g, LiquidGlassRenderer.status(), left + 14, top + 206, IslandUi.MUTED);
        if (!error.isEmpty()) g.centeredText(font, error, width / 2, height - 20, 0xFFFF9999);
    }
    @Override public void onClose() {
        try { IslandClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
        catch (Exception e) { error = "保存失败，请检查配置目录权限"; }
    }
    @Override public boolean isPauseScreen() { return false; }
}

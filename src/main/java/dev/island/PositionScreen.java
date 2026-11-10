package dev.island;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class PositionScreen extends Screen {
    private final Screen parent;
    private boolean moving;
    private double offsetX, offsetY;
    private IslandUi.Slider horizontal, vertical;
    private String error = "";
    public PositionScreen(Screen parent) { super(Component.literal("移动灵动岛")); this.parent = parent; }
    @Override protected void init() {
        int w = Math.min(360, width - 24), x = (width - w) / 2, y = height - 86;
        horizontal = addRenderableWidget(new IslandUi.Slider(x, y, (w - 8) / 2, IslandClient.CONFIG.x) {
            { updateMessage(); }
            protected void updateMessage() { setMessage(Component.literal("水平：" + Math.round(value * 100) + "%")); }
            protected void applyValue() { IslandClient.CONFIG.x = value; }
        });
        vertical = addRenderableWidget(new IslandUi.Slider(x + (w + 8) / 2, y, (w - 8) / 2, IslandClient.CONFIG.y) {
            { updateMessage(); }
            protected void updateMessage() { setMessage(Component.literal("垂直：" + Math.round(value * 100) + "%")); }
            protected void applyValue() { IslandClient.CONFIG.y = value; }
        });
        addRenderableWidget(new IslandUi.Action("顶部居中", x, y + 26, (w - 8) / 2, false, () -> {
            IslandClient.CONFIG.x = .5; IslandClient.CONFIG.y = .035; sync();
        }));
        addRenderableWidget(new IslandUi.Action("保存并返回", x + (w + 8) / 2, y + 26, (w - 8) / 2, true, this::onClose));
    }
    private void sync() { horizontal.sync(IslandClient.CONFIG.x); vertical.sync(IslandClient.CONFIG.y); }
    private void move(double x, double y) {
        var box = IslandClient.previewBounds();
        IslandClient.CONFIG.x = IslandPlacement.anchorX(x - offsetX, box.width(), width);
        IslandClient.CONFIG.y = IslandPlacement.anchorY(y - offsetY, height); sync();
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean twice) {
        if (super.mouseClicked(e, twice)) return true;
        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT && IslandClient.previewBounds().contains(e.x(), e.y())) {
            var box = IslandClient.previewBounds(); offsetX = e.x() - box.x(); offsetY = e.y() - box.y();
            moving = true; setDragging(true); return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (moving && e.button() == InputConstants.MOUSE_BUTTON_LEFT) { move(e.x(), e.y()); return true; }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT && moving) { moving = false; setDragging(false); return true; }
        return super.mouseReleased(e);
    }
    @Override public boolean keyPressed(KeyEvent e) {
        int key = e.key();
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
            if (key == InputConstants.KEY_LEFT) IslandClient.CONFIG.x = Math.clamp(IslandClient.CONFIG.x - 1.0 / width, 0, 1);
            if (key == InputConstants.KEY_RIGHT) IslandClient.CONFIG.x = Math.clamp(IslandClient.CONFIG.x + 1.0 / width, 0, 1);
            if (key == InputConstants.KEY_UP) IslandClient.CONFIG.y = Math.clamp(IslandClient.CONFIG.y - 1.0 / height, 0, 1);
            if (key == InputConstants.KEY_DOWN) IslandClient.CONFIG.y = Math.clamp(IslandClient.CONFIG.y + 1.0 / height, 0, 1);
            sync(); return true;
        }
        return super.keyPressed(e);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        IslandUi.backdrop(g);
        // Keep the world sharp and unobscured to make the actual location obvious.
        g.fill(0, height - 112, width, height, 0xC0101726);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        IslandClient.draw(g, true);
        var box = IslandClient.previewBounds();
        g.outline(box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4, moving ? 0xFFF1F4FF : 0xFF8EACDF);
        g.centeredText(font, "拖动灵动岛 · 方向键微调 · 滑块定位 · Esc 保存", width / 2, height - 105, IslandUi.TEXT);
        if (!error.isEmpty()) g.centeredText(font, error, width / 2, height - 24, 0xFFFF9999);
    }
    @Override public void onClose() {
        try { IslandClient.CONFIG.save(); minecraft.gui.setScreen(parent); }
        catch (Exception e) { error = "保存失败，请检查配置目录权限"; }
    }
    @Override public boolean isPauseScreen() { return false; }
}

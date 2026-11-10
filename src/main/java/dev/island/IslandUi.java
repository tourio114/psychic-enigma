package dev.island;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared restrained glass cards, buttons and sliders; all coordinates use scaled GUI units. */
public final class IslandUi {
    public static final int TEXT = 0xFFF1F4FF, MUTED = 0xFF9EABC5, ACCENT = 0xFFADC7FF;
    private IslandUi() {}
    public static void backdrop(GuiGraphicsExtractor g) {
        if (Minecraft.getInstance().level == null) {
            float spin = IslandClient.CONFIG.animations ? (float) ((System.nanoTime() / 1e9 * .6) % 360) : 15;
            ((dev.island.mixin.GuiGraphicsAccess) g).island$state().panoramaRenderState =
                    new net.minecraft.client.renderer.state.gui.PanoramaRenderState(spin);
        }
    }
    public static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int row = 0; row < h; row++) {
            double dy = row < r ? r - row - .5 : row >= h - r ? row - (h - r) + .5 : 0;
            int inset = dy == 0 ? 0 : (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }
    public static final class Action extends Button {
        private final boolean accent;
        public Action(String label, int x, int y, int w, boolean accent, Runnable action) {
            super(x, y, w, 20, Component.literal(label), b -> action.run(), DEFAULT_NARRATION); this.accent = accent;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hover = isHoveredOrFocused();
            int bg = !active ? 0x202A344B : accent ? hover ? 0xE04E6B9D : 0xB037507C : hover ? 0xA044516B : 0x70303B51;
            round(g, getX(), getY(), width, height, 6, bg);
            if (isFocused()) g.outline(getX(), getY(), width, height, 0xFFADC7FF);
            var font = Minecraft.getInstance().font;
            String text = font.plainSubstrByWidth(getMessage().getString(), Math.max(1, width - 10));
            g.text(font, text, getX() + (width - font.width(text)) / 2, getY() + 6, active ? TEXT : MUTED, false);
        }
    }
    public abstract static class Slider extends AbstractSliderButton {
        public Slider(int x, int y, int width, double value) { super(x, y, width, 20, Component.empty(), value); }
        public void sync(double next) { value = Math.clamp(next, 0, 1); updateMessage(); }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            round(g, getX(), getY(), width, 20, 6, 0x80303B51);
            int handle = getX() + 5 + (int) Math.round(value * (width - 10));
            g.fill(getX() + 5, getY() + 17, getX() + width - 5, getY() + 18, 0xFF4B5873);
            g.fill(getX() + 5, getY() + 17, handle, getY() + 18, ACCENT);
            round(g, handle - 2, getY() + 15, 4, 5, 2, isHoveredOrFocused() ? TEXT : ACCENT);
            var font = Minecraft.getInstance().font;
            g.text(font, getMessage(), getX() + 8, getY() + 4, TEXT, false);
        }
    }
    public abstract static class CardScreen extends Screen {
        protected int left, top, cardWidth, cardHeight;
        protected String caption;
        protected CardScreen(String title, String caption) { super(Component.literal(title)); this.caption = caption; }
        protected void layout() {
            cardWidth = Math.min(440, width - 20); cardHeight = Math.min(320, height - 20);
            left = (width - cardWidth) / 2; top = (height - cardHeight) / 2;
        }
        protected Action action(String text, int x, int y, int w, boolean accent, Runnable action) {
            return addRenderableWidget(new Action(text, x, y, w, accent, action));
        }
        protected void label(GuiGraphicsExtractor g, String text, int x, int y, int color) {
            g.text(font, font.plainSubstrByWidth(text, cardWidth - 28), x, y, color, false);
        }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
            backdrop(g);
            g.fill(0, 0, width, height, 0x60050A13);
            round(g, left - 2, top + 4, cardWidth + 4, cardHeight, 16, 0x50000000);
            round(g, left, top, cardWidth, cardHeight, 14, 0xDE111827);
            if (IslandClient.CONFIG.glass) LiquidGlassRenderer.enqueue(g, left, top, cardWidth, cardHeight, 14, .75);
            g.fill(left + 18, top + 1, left + cardWidth - 18, top + 2, 0x607E9ACB);
            label(g, title.getString(), left + 14, top + 12, TEXT);
            label(g, caption, left + 14, top + 28, MUTED);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}

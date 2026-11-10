package dev.island;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class IslandClient implements ClientModInitializer {
    public static final IslandConfig CONFIG = IslandConfig.load();
    public static MusicService MUSIC;
    public static KeyMapping OPEN;
    private static LivingEntity target;
    private static long until;
    private static float health = Float.NaN, maxHealth;
    private static final Visual HUD = new Visual(), PREVIEW = new Visual();
    private static final class Visual {
        final IslandMotion motion = new IslandMotion();
        long lastFrame;
        String primary = "", secondary = "", outgoing = "", outgoingSecondary = "";
    }
    @Override public void onInitializeClient() {
        MUSIC = new MusicService(CONFIG);
        OPEN = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dynamicisland.config", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dynamicisland", "main"))));
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BEFORE_INIT.register((client, screen, sw, sh) -> {
            if (screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
                net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.beforeKeyPress(screen).register((s, event) -> {
                    if (OPEN.matches(event)) client.gui.setScreen(new ConfigScreen());
                });
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.consumeClick()) if (client.player != null) client.gui.setScreen(new ConfigScreen());
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player == Minecraft.getInstance().player && !player.isSpectator() && entity instanceof LivingEntity living) {
                if (target != living) health = Float.NaN;
                HUD.motion.hit();
                target = living; maxHealth = living.getMaxHealth(); until = System.nanoTime() + 5_000_000_000L;
            }
            return InteractionResult.PASS;
        });
        ClientPlayNetworking.registerGlobalReceiver(HealthPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (target != null && target.getId() == payload.entityId()) {
                health = payload.health(); maxHealth = payload.maxHealth();
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { target = null; health = Float.NaN; });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MUSIC.close());
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("dynamicisland", "island"), (g, delta) -> draw(g, false));
    }
    private static IslandPlacement.Box previewBox = new IslandPlacement.Box(0, 0, 112, 28);
    public static IslandPlacement.Box previewBounds() { return previewBox; }
    private static int previewTop = 10;
    public static void drawPreviewAt(GuiGraphicsExtractor g, int top) { previewTop = top; draw(g, true, true); }
    public static void drawPreview(GuiGraphicsExtractor g) { drawPreviewAt(g, 10); }
    public static void draw(GuiGraphicsExtractor g, boolean preview) { draw(g, preview, false); }
    private static void draw(GuiGraphicsExtractor g, boolean preview, boolean centered) {
        Minecraft mc = Minecraft.getInstance();
        if (!preview && (mc.player == null || mc.gui.hud.isHidden())) return;
        boolean combat = !preview && target != null && System.nanoTime() < until;
        boolean music = MUSIC.audio.playing;
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern(CONFIG.seconds ? "HH:mm:ss" : "HH:mm"));
        String primary = combat ? target.getName().getString() : music ? MUSIC.audio.title : time;
        String secondary = combat ? Float.isNaN(health) ? "血量未同步 · 服务端需安装本模组"
                : String.format(Locale.ROOT, "%.1f / %.1f HP", Math.max(0, health), maxHealth)
                : MUSIC.audio.paused() ? "已暂停 · " + time : "正在播放 · " + time;
        Visual v = preview ? PREVIEW : HUD;
        long now = System.nanoTime();
        double elapsed = v.lastFrame == 0 ? 1 : (now - v.lastFrame) / 1e9;
        if (elapsed > .25) v.motion.enter();
        v.lastFrame = now;
        String identity = combat ? "combat:" + target.getId() : music ? "music:" + MUSIC.audio.title : "clock";
        if (v.motion.change(identity)) {
            v.outgoing = v.primary; v.outgoingSecondary = v.secondary;
        }
        v.primary = primary; v.secondary = combat || music ? secondary : "";
        v.motion.step(elapsed > .25 ? .016 : elapsed, combat || music,
                combat && maxHealth > 0 ? health / maxHealth : 1, CONFIG.animations);
        IslandMotion motion = v.motion;
        int w = (int) Math.round(motion.width), h = (int) Math.round(motion.height);
        int sw = g.guiWidth(), sh = g.guiHeight();
        w = Math.min(w, sw);
        double appear = IslandMotion.smooth(motion.appearance);
        var placement = IslandPlacement.box(centered ? .5 : CONFIG.x, centered ? previewTop / (double) sh : CONFIG.y, sw, sh, w, h);
        int x = placement.x();
        int y = placement.y() - (preview ? 0 : (int) Math.round((1 - appear) * 7));
        if (preview) previewBox = placement;
        glass(g, x, y, w, h, appear, motion.pulse);
        // Clip sliding text to the interior, keeping it inside the animated capsule.
        g.enableScissor(x + 10, y + 4, x + w - 10, y + h - 5);
        double t = IslandMotion.smooth(motion.transition);
        if (t < 1 && !v.outgoing.isEmpty()) {
            text(g, v.outgoing, v.outgoingSecondary, x, y - (int) Math.round(t * 6), w, h, (1 - t) * appear);
        }
        text(g, primary, v.secondary, x, y + (int) Math.round((1 - t) * 6), w, h, t * appear);
        g.disableScissor();
        double detail = IslandMotion.smooth((h - 34) / 20.0) * appear * t;
        if (detail > .01 && (combat || music)) {
            if (combat && Float.isFinite(health) && maxHealth > 0) {
                g.fill(x + 12, y + h - 9, x + w - 12, y + h - 6, color(0xD5DDEB, .16 * detail));
                int end = x + 12 + (int) ((w - 24) * motion.health);
                g.fill(x + 12, y + h - 9, end, y + h - 6, color(0x76EDAF, detail));
                if (end > x + 12) g.fill(x + 12, y + h - 9, end, y + h - 8, color(0xE5FFEF, .65 * detail));
            } else if (music) {
                for (int i = 0; i < 12; i++) {
                    int bar = MUSIC.audio.paused() || !CONFIG.animations ? 3
                            : 2 + (int) (3 * (1 + Math.sin(now / 180_000_000.0 + i * .7)));
                    g.fill(x + w - 52 + i * 3, y + h - 5 - bar, x + w - 51 + i * 3,
                            y + h - 5, color(0xC5B5FF, detail));
                }
            }
        }
    }
    private static void text(GuiGraphicsExtractor g, String primary, String secondary,
                             int x, int y, int w, int h, double alpha) {
        if (alpha < .025) return;
        var font = Minecraft.getInstance().font;
        String trimmed = font.plainSubstrByWidth(primary, Math.max(1, w - 28));
        g.text(font, trimmed, x + (w - font.width(trimmed)) / 2, y + 10, color(0xF6F7FF, alpha), false);
        double details = IslandMotion.smooth((h - 34) / 20.0);
        if (!secondary.isEmpty() && details * alpha > .025)
            g.text(font, font.plainSubstrByWidth(secondary, Math.max(1, w - 24)), x + 12, y + 27,
                    color(0xCED2E2, alpha * details), false);
    }
    private static int color(int rgb, double opacity) {
        return ((int) Math.round(255 * Math.clamp(opacity, 0, 1)) << 24) | (rgb & 0xFFFFFF);
    }
    private static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h, double alpha, double pulse) {
        double opacity = Math.clamp(CONFIG.opacity, 0, 1);
        // Opacity controls the entire panel surface, including sheen, shadow and border, not text.
        double surface = alpha * opacity;
        for (int spread = 4; spread >= 1; spread--)
            pill(g, x - spread, y - spread + 3, w + spread * 2, h + spread * 2, color(0x000000, .035 * surface));
        pill(g, x, y, w, h, color(0x0C101A, surface));
        if (!CONFIG.glass) return;
        LiquidGlassRenderer.enqueue(g, x, y, w, h, Math.min(14, h / 2), surface);
        // A translucent glass treatment; no full-screen blur pass or framebuffer readback.
        for (int row = 0; row < h; row++) {
            int inset = inset(row, h);
            double highlight = .15 * Math.pow(1 - row / (double) h, 3) + .015;
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color(0xD3E0FF, highlight * surface));
            if (w > 2 * inset + 2) {
                g.fill(x + inset, y + row, x + inset + 1, y + row + 1, color(0xE8EEFF, (.18 + .22 * pulse) * surface));
                g.fill(x + w - inset - 1, y + row, x + w - inset, y + row + 1, color(0xCBD9FF, (.10 + .15 * pulse) * surface));
            }
            // Stable grain avoids flicker while the camera moves; sparsely spaced for a low draw cost.
            for (int col = inset + 3; col < w - inset - 3; col += 7) {
                int hash = (col * 73 + row * 151) & 255;
                if (hash < 44) g.fill(x + col, y + row, x + col + 1, y + row + 1,
                        color(0xFFFFFF, .045 * surface));
            }
        }
        g.fill(x + 14, y, x + w - 14, y + 1, color(0xF2F5FF, (.32 + .3 * pulse) * surface));
        g.fill(x + 14, y + h - 1, x + w - 14, y + h, color(0xAEBBDA, .12 * surface));
    }
    private static int inset(int row, int h) {
        int r = Math.min(14, h / 2);
        double dy = row < r ? r - row - .5 : row >= h - r ? row - (h - r) + .5 : 0;
        return dy == 0 ? 0 : (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }
    private static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        for (int row = 0; row < h; row++) {
            int inset = inset(row, h);
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }
}

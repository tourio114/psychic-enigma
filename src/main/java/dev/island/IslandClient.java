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
    private static double animatedWidth = 112, animatedHeight = 28;
    private static long lastFrame;
    @Override public void onInitializeClient() {
        MUSIC = new MusicService(CONFIG);
        OPEN = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dynamicisland.config", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dynamicisland", "main"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.consumeClick()) if (client.player != null) client.gui.setScreen(new ConfigScreen());
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player == Minecraft.getInstance().player && !player.isSpectator() && entity instanceof LivingEntity living) {
                if (target != living) health = Float.NaN;
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
    public static void draw(GuiGraphicsExtractor g, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        if (!preview && (mc.player == null || mc.gui.hud.isHidden())) return;
        boolean combat = !preview && target != null && System.nanoTime() < until;
        boolean music = MUSIC.audio.playing;
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern(CONFIG.seconds ? "HH:mm:ss" : "HH:mm"));
        String primary = combat ? target.getName().getString() : music ? MUSIC.audio.title : time;
        String secondary = combat ? Float.isNaN(health) ? "血量未同步 · 服务端需安装本模组"
                : String.format(Locale.ROOT, "%.1f / %.1f HP", Math.max(0, health), maxHealth)
                : MUSIC.audio.paused() ? "已暂停 · " + time : "正在播放 · " + time;
        int desiredWidth = combat || music ? 236 : 112;
        int desiredHeight = combat || music ? 54 : 28;
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? .016 : Math.min(.1, (now - lastFrame) / 1e9); lastFrame = now;
        double ease = 1 - Math.exp(-14 * dt);
        animatedWidth += (desiredWidth - animatedWidth) * ease;
        animatedHeight += (desiredHeight - animatedHeight) * ease;
        int w = (int) animatedWidth, h = (int) animatedHeight;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int x = Math.clamp((int) (CONFIG.x * sw) - w / 2, 0, Math.max(0, sw - w));
        int y = Math.clamp((int) (CONFIG.y * sh), 0, Math.max(0, sh - h));
        pill(g, x, y + 2, w, h, 0x40000000);
        pill(g, x, y, w, h, 0xF208090C);
        String trimmed = mc.font.plainSubstrByWidth(primary, Math.max(1, w - 28));
        g.text(mc.font, trimmed, x + (w - mc.font.width(trimmed)) / 2, y + 10, 0xFFF4F4F8, false);
        if (h >= 48 && (combat || music)) {
            g.text(mc.font, mc.font.plainSubstrByWidth(secondary, w - 24), x + 12, y + 27, 0xFFAFB0BC, false);
            if (combat && Float.isFinite(health) && maxHealth > 0) {
                g.fill(x + 12, y + h - 9, x + w - 12, y + h - 6, 0xFF30313A);
                g.fill(x + 12, y + h - 9, x + 12 + (int) ((w - 24) * Math.clamp(health / maxHealth, 0, 1)), y + h - 6, 0xFF76EDAF);
            } else if (music) {
                for (int i = 0; i < 12; i++) {
                    int bar = MUSIC.audio.paused() ? 2 : 2 + (int) (3 * (1 + Math.sin(now / 180_000_000.0 + i)));
                    g.fill(x + w - 52 + i * 3, y + h - 5 - bar, x + w - 51 + i * 3, y + h - 5, 0xFFAC94FF);
                }
            }
        }
    }
    private static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        int r = Math.min(14, h / 2);
        for (int row = 0; row < h; row++) {
            double dy = row < r ? r - row - .5 : row >= h - r ? row - (h - r) + .5 : 0;
            int inset = dy == 0 ? 0 : (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }
}

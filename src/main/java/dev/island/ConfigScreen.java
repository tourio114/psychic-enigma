package dev.island;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import com.google.zxing.common.BitMatrix;
import java.util.*;

public final class ConfigScreen extends Screen {
    private EditBox api, keywords;
    private List<MusicService.Song> results = List.of();
    private final List<Button> songs = new ArrayList<>();
    private boolean musicPage;
    private int searchGeneration;
    private String message = "";
    public ConfigScreen() { super(Component.literal("灵动岛设置")); }
    private Button button(String text, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, 20).build());
    }
    @Override protected void init() {
        int x = width / 2 - 150;
        button(musicPage ? "返回设置" : "音乐播放器", x, 32, 100, () -> {
            musicPage = !musicPage; rebuildWidgets();
        });
        button("完成", x + 220, 32, 80, this::onClose);
        if (!musicPage) {
            api = addRenderableWidget(new EditBox(font, x, 74, 300, 20, Component.literal("API 地址")));
            api.setMaxLength(512); api.setValue(IslandClient.CONFIG.api);
            button("保存 API", x, 100, 96, this::saveApi);
            button("扫码登录", x + 102, 100, 96, () -> { if (saveApi()) IslandClient.MUSIC.login(); });
            button("退出登录", x + 204, 100, 96, () -> IslandClient.MUSIC.logout());
            button("时间显示秒数", x, 128, 96, () -> { IslandClient.CONFIG.seconds = !IslandClient.CONFIG.seconds; saveConfig(); });
            button("拖动设置位置", x + 102, 128, 96, () -> minecraft.gui.setScreen(new PositionScreen(this)));
            button("修改快捷键", x + 204, 128, 96, () -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options)));
            button("音量 −", x, 156, 96, () -> volume(-.1f));
            button("音量 +", x + 102, 156, 96, () -> volume(.1f));
            return;
        }
        keywords = addRenderableWidget(new EditBox(font, x, 62, 222, 20, Component.literal("搜索歌曲")));
        keywords.setMaxLength(100);
        button("搜索", x + 228, 62, 72, this::search);
        songs.clear();
        for (int i = 0; i < 5; i++) {
            final int index = i;
            songs.add(button("", x, 86 + 21 * i, 300, () -> {
                if (index < results.size()) IslandClient.MUSIC.play(results.get(index));
            }));
        }
        updateResults();
        button("暂停 / 继续", x, 194, 145, () -> IslandClient.MUSIC.audio.togglePause());
        button("停止", x + 155, 194, 145, () -> IslandClient.MUSIC.stop());
    }
    private void saveConfig() {
        try { IslandClient.CONFIG.save(); message = "设置已保存"; }
        catch (Exception e) { message = "保存失败：请检查配置目录权限"; }
    }
    private boolean saveApi() {
        String next = api.getValue().strip();
        try {
            var u = java.net.URI.create(next);
            if (u.getHost() == null || !("https".equals(u.getScheme()) || "http".equals(u.getScheme()))
                    || u.getUserInfo() != null || u.getQuery() != null || u.getFragment() != null) throw new IllegalArgumentException();
            if (!next.equals(IslandClient.CONFIG.api)) {
                IslandClient.MUSIC.cancelLogin(); IslandClient.MUSIC.stop();
                IslandClient.CONFIG.api = next;
                message = "API 已更改，原地址的凭证不会发送至新地址";
            }
            saveConfig(); return true;
        } catch (Exception e) { message = "请输入有效的 HTTP(S) API 地址"; return false; }
    }
    private void volume(float delta) {
        IslandClient.CONFIG.volume = Math.clamp(IslandClient.CONFIG.volume + delta, 0, 1);
        IslandClient.MUSIC.audio.volume = IslandClient.CONFIG.volume; saveConfig();
    }
    private void search() {
        if (keywords.getValue().isBlank()) return;
        int version = ++searchGeneration; message = "搜索中…";
        try {
            IslandClient.MUSIC.search(keywords.getValue().strip()).whenComplete((items, error) -> minecraft.execute(() -> {
                if (version != searchGeneration || !musicPage) return;
                if (error != null) { message = MusicService.safe(error); return; }
                results = items; message = items.isEmpty() ? "未找到歌曲" : "点击歌曲播放"; updateResults();
            }));
        } catch (Exception e) { message = MusicService.safe(e); }
    }
    private void updateResults() {
        for (int i = 0; i < songs.size(); i++) {
            Button b = songs.get(i); b.visible = i < results.size();
            if (b.visible) b.setMessage(Component.literal(font.plainSubstrByWidth(results.get(i).name() + " · " + results.get(i).artist(), 280)));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = width / 2 - 150;
        g.text(font, "灵动岛 · 设置与网易云音乐", x, 18, 0xFFFFFFFF, false);
        if (!musicPage) g.text(font, "API 地址（需先部署服务）", x, 61, 0xFFBDBDC8, false);
        g.text(font, font.plainSubstrByWidth(message, width - 20), 10, height - 23, 0xFFB8D6FF, false);
        String state = IslandClient.MUSIC.audio.error.isEmpty() ? IslandClient.MUSIC.status : IslandClient.MUSIC.audio.error;
        g.text(font, font.plainSubstrByWidth(state, width - 20), 10, height - 11, 0xFFBDBDC8, false);
        BitMatrix qr = IslandClient.MUSIC.qr;
        if (qr != null) {
            // Modal login overlay. Click to dismiss; closing this screen cancels polling too.
            g.fill(0, 0, width, height, 0xED101018);
            int scale = Math.max(1, Math.min(4, (height - 80) / qr.getHeight()));
            int size = qr.getWidth() * scale, qx = (width - size) / 2, qy = (height - size) / 2;
            g.fill(qx, qy, qx + size, qy + size, 0xFFFFFFFF);
            for (int yy = 0; yy < qr.getHeight(); yy++) for (int xx = 0; xx < qr.getWidth(); xx++)
                if (qr.get(xx, yy)) g.fill(qx + xx * scale, qy + yy * scale, qx + (xx + 1) * scale, qy + (yy + 1) * scale, 0xFF000000);
            g.text(font, font.plainSubstrByWidth(IslandClient.MUSIC.status, width - 20), 10, 12, 0xFFFFFFFF, false);
            g.text(font, "点击任意位置关闭二维码", 10, height - 20, 0xFFFFFFFF, false);
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (IslandClient.MUSIC.qr != null) { IslandClient.MUSIC.cancelLogin(); return true; }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public void onClose() { saveConfig(); minecraft.gui.setScreen(null); }
    @Override public void removed() { ++searchGeneration; IslandClient.MUSIC.cancelLogin(); }
    @Override public boolean isPauseScreen() { return false; }

    private static final class PositionScreen extends Screen {
        private final ConfigScreen parent;
        private boolean dragging;
        PositionScreen(ConfigScreen parent) { super(Component.literal("拖动灵动岛")); this.parent = parent; }
        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.literal("恢复顶部居中"), b -> {
                IslandClient.CONFIG.x = .5; IslandClient.CONFIG.y = .035;
            }).bounds(width / 2 - 110, height - 35, 130, 20).build());
            addRenderableWidget(Button.builder(Component.literal("完成"), b -> onClose()).bounds(width / 2 + 30, height - 35, 80, 20).build());
        }
        private void position(double x, double y) {
            IslandClient.CONFIG.x = Math.clamp(x / width, 0, 1);
            IslandClient.CONFIG.y = Math.clamp((y - 14) / height, 0, 1);
        }
        @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) return true;
            if (event.button() == 0) { dragging = true; position(event.x(), event.y()); return true; }
            return false;
        }
        @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            if (dragging) { position(event.x(), event.y()); return true; }
            return super.mouseDragged(event, dx, dy);
        }
        @Override public boolean mouseReleased(MouseButtonEvent event) { dragging = false; return super.mouseReleased(event); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            super.extractRenderState(g, mouseX, mouseY, delta);
            IslandClient.draw(g, true);
            g.text(font, "按住左键拖动，Esc 保存并返回", 10, height - 55, 0xFFFFFFFF, false);
        }
        @Override public void onClose() {
            try { IslandClient.CONFIG.save(); } catch (Exception ignored) {}
            minecraft.gui.setScreen(parent);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}

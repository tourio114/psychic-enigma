package dev.island;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import com.google.zxing.common.BitMatrix;
import java.util.*;

public final class ConfigScreen extends IslandUi.CardScreen {
    private EditBox api, keywords;
    private List<MusicService.Song> results = List.of();
    private final List<Button> songs = new ArrayList<>();
    private boolean musicPage;
    private int searchGeneration, offset;
    private String apiDraft = IslandClient.CONFIG.api, keywordDraft = "";
    private String message = "";
    public ConfigScreen() { super("灵动岛", "DYNAMIC ISLAND  /  个性化你的游戏 HUD"); }
    private Button button(String text, int x, int y, int w, Runnable action) {
        return action(text, x, y, w, false, action);
    }
    private void switchPage(boolean music) {
        if (api != null) apiDraft = api.getValue();
        if (keywords != null) keywordDraft = keywords.getValue();
        musicPage = music; rebuildWidgets();
    }
    @Override protected void init() {
        layout();
        int x = left + 14, y = top, full = cardWidth - 28, half = (full - 8) / 2;
        action("关闭", left + cardWidth - 66, y + 10, 52, false, this::onClose);
        action("常规设置", x, y + 46, half, !musicPage, () -> switchPage(false));
        action("音乐与账号", x + half + 8, y + 46, full - half - 8, musicPage, () -> switchPage(true));
        songs.clear();
        if (!musicPage) {
            action("外观与动画", x, y + 82, half, true, () -> minecraft.gui.setScreen(new AppearanceScreen(this)));
            action("位置与布局", x + half + 8, y + 82, full - half - 8, false, () -> minecraft.gui.setScreen(new PositionScreen(this)));
            action("显示秒数：" + (IslandClient.CONFIG.seconds ? "开" : "关"), x, y + 112, half, false, () -> {
                IslandClient.CONFIG.seconds = !IslandClient.CONFIG.seconds; saveConfig(); rebuildWidgets();
            });
            action("快捷键设置", x + half + 8, y + 112, full - half - 8, false,
                    () -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options)));
            addRenderableWidget(new IslandUi.Slider(x, y + 148, full, IslandClient.CONFIG.volume) {
                { updateMessage(); }
                protected void updateMessage() { setMessage(Component.literal("音乐音量：" + Math.round(value * 100) + "%")); }
                protected void applyValue() {
                    IslandClient.CONFIG.volume = (float) value; IslandClient.MUSIC.audio.volume = (float) value;
                }
            });
            return;
        }
        api = addRenderableWidget(new EditBox(font, x, y + 74, full, 20, Component.literal("API 地址")));
        api.setMaxLength(512); api.setValue(apiDraft); api.setHint(Component.literal("填写网易云 API 地址"));
        int third = (full - 12) / 3;
        button("保存接口", x, y + 100, third, this::saveApi);
        action("扫码登录", x + third + 6, y + 100, third, true, () -> { if (saveApi()) IslandClient.MUSIC.login(); });
        button("退出账号", x + (third + 6) * 2, y + 100, full - (third + 6) * 2, () -> IslandClient.MUSIC.logout());
        keywords = addRenderableWidget(new EditBox(font, x, y + 126, full - 58, 20, Component.literal("搜索歌曲")));
        keywords.setMaxLength(100); keywords.setValue(keywordDraft); keywords.setHint(Component.literal("搜索歌曲或歌手"));
        action("搜索", x + full - 52, y + 126, 52, true, this::search);
        int count = Math.clamp((cardHeight - 192) / 22, 1, 5);
        for (int i = 0; i < count; i++) {
            final int index = i;
            songs.add(button("", x, y + 152 + 22 * i, full, () -> {
                int n = offset + index;
                if (n < results.size()) IslandClient.MUSIC.play(results.get(n));
            }));
        }
        updateResults();
        int bottom = y + 156 + count * 22;
        int playWidth = (full - 100) / 2;
        button("暂停 / 继续", x, bottom, playWidth, () -> IslandClient.MUSIC.audio.togglePause());
        button("停止", x + playWidth + 6, bottom, playWidth, () -> IslandClient.MUSIC.stop());
        button("上一页", x + full - 88, bottom, 42, () -> { offset = Math.max(0, offset - count); updateResults(); });
        button("下一页", x + full - 42, bottom, 42, () -> {
            if (offset + count < results.size()) offset += count; updateResults();
        });
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
                IslandClient.CONFIG.api = next; apiDraft = next;
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
                results = items; offset = 0; message = items.isEmpty() ? "未找到歌曲" : "点击歌曲播放"; updateResults();
            }));
        } catch (Exception e) { message = MusicService.safe(e); }
    }
    private void updateResults() {
        for (int i = 0; i < songs.size(); i++) {
            Button b = songs.get(i); b.visible = offset + i < results.size();
            if (b.visible) b.setMessage(Component.literal(font.plainSubstrByWidth(results.get(offset + i).name() + " · " + results.get(offset + i).artist(), cardWidth - 48)));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        caption = musicPage && !message.isEmpty() ? message : "DYNAMIC ISLAND  /  个性化你的游戏 HUD";
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (!musicPage && cardHeight >= 270) {
            label(g, "外观与位置实时预览，设置会在退出时保存。", left + 14, top + 192, IslandUi.MUTED);
            label(g, LiquidGlassRenderer.status(), left + 14, top + 210, IslandUi.ACCENT);
        }
        if (!musicPage) label(g, message, left + 14, top + cardHeight - 25, IslandUi.ACCENT);
        String state = IslandClient.MUSIC.audio.error.isEmpty() ? IslandClient.MUSIC.status : IslandClient.MUSIC.audio.error;
        label(g, state, left + 14, top + cardHeight - 13, IslandUi.MUTED);
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

}

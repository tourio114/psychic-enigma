package dev.island;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class InfoSettingsScreen extends IslandUi.CardScreen {
    private final Screen parent;
    private String error = "";
    public InfoSettingsScreen(Screen parent) { super("战斗与 Tab 信息", "显示时长 / 玩家延迟 / 服务器状态"); this.parent = parent; }
    @Override protected void init() {
        layout(); int x = left + 14, full = cardWidth - 28;
        addRenderableWidget(new IslandUi.Slider(x, top + 60, full, (IslandClient.CONFIG.attackDisplaySeconds - 1) / 29.0) {
            { updateMessage(); }
            protected void updateMessage() { setMessage(Component.literal("攻击目标显示时长：" + Math.round(1 + value * 29) + " 秒")); }
            protected void applyValue() { IslandClient.CONFIG.attackDisplaySeconds = Math.round(1 + value * 29); }
        });
        action("Tab 信息：" + (IslandClient.CONFIG.tabInfo ? "开" : "关"), x, top + 88, full, false, () -> {
            IslandClient.CONFIG.tabInfo = !IslandClient.CONFIG.tabInfo; rebuildWidgets();
        });
        action("显示服务器地址：" + (IslandClient.CONFIG.showServerAddress ? "开" : "关"), x, top + 116, full, false, () -> {
            IslandClient.CONFIG.showServerAddress = !IslandClient.CONFIG.showServerAddress; rebuildWidgets();
        });
        action("保存并返回", x, top + 154, full, true, this::onClose);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        if (cardHeight >= 260) label(g, "时长对之后的攻击生效；MSPT 需要服务端同步。", left + 14, top + 200, IslandUi.MUTED);
        if (!error.isEmpty()) label(g, error, left + 14, top + cardHeight - 16, 0xFFFF9999);
    }
    @Override public void onClose() {
        try { IslandClient.CONFIG.save(); IslandClient.syncDuration(); minecraft.gui.setScreen(parent); }
        catch (Exception e) { error = "设置保存失败，请检查配置或网络状态"; }
    }
}

package dev.island;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.Locale;

public final class ServerTabInfo {
    private static ServerInfoPayload status;
    private static long received;
    public static void update(ServerInfoPayload next) { status = next; received = System.nanoTime(); }
    public static void clear() { status = null; received = 0; }
    public static MutableComponent header(Minecraft client) {
        var connection = client.getConnection();
        if (connection == null) return Component.literal("服务器信息不可用");
        var data = client.getCurrentServer();
        String name = client.isLocalServer() ? "单人世界" : data == null ? "服务器" : data.name;
        String brand = connection.serverBrand();
        var local = client.player == null ? null : connection.getPlayerInfo(client.player.getUUID());
        String ping = local == null ? "--" : InfoRules.latency(local.getLatency());
        boolean fresh = status != null && System.nanoTime() - received < 4_000_000_000L;
        String players = fresh ? "在线 " + status.online() + "/" + status.maximum()
                : "列表玩家 " + connection.getListedOnlinePlayers().size();
        String version = data != null && data.version != null ? data.version.getString() : "26.3";
        MutableComponent result = Component.literal(name + " · " + (brand == null ? "未知类型" : brand)).withStyle(ChatFormatting.AQUA);
        result.append(Component.literal("\n当前延迟 " + ping + "  ·  " + players + "  ·  版本 " + version).withStyle(ChatFormatting.GRAY));
        String timing = fresh && Float.isFinite(status.mspt()) && status.mspt() >= 0 && Float.isFinite(status.targetTps())
                ? String.format(Locale.ROOT, "平均 MSPT %.2f  ·  目标 TPS %.1f", status.mspt(), Math.max(0, status.targetTps()))
                : "MSPT / 目标 TPS：服务端未同步";
        result.append(Component.literal("\n" + timing).withStyle(ChatFormatting.GRAY));
        if (IslandClient.CONFIG.showServerAddress && data != null)
            result.append(Component.literal("\n地址 " + data.ip).withStyle(ChatFormatting.GRAY));
        return result;
    }
    private ServerTabInfo() {}
}

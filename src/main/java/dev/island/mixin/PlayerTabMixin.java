package dev.island.mixin;

import dev.island.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extend the original tab list, retaining teams, skins, scoreboards and server-provided header/footer. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabMixin {
    @Shadow private Component header;
    @Unique private Component island$originalHeader;
    @Unique private boolean island$replaced;
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void island$header(GuiGraphicsExtractor g, int width, Scoreboard board, Objective objective, CallbackInfo ci) {
        if (!IslandClient.CONFIG.tabInfo) return;
        island$originalHeader = header; island$replaced = true;
        var info = ServerTabInfo.header(Minecraft.getInstance());
        if (header != null) info.append("\n\n").append(header);
        header = info;
    }
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void island$restore(GuiGraphicsExtractor g, int width, Scoreboard board, Objective objective, CallbackInfo ci) {
        if (island$replaced) { header = island$originalHeader; island$originalHeader = null; island$replaced = false; }
    }
    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 13))
    private int island$pingWidth(int vanilla) { return IslandClient.CONFIG.tabInfo ? 46 : vanilla; }
    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void island$ping(GuiGraphicsExtractor g, int width, int x, int y, PlayerInfo info, CallbackInfo ci) {
        if (!IslandClient.CONFIG.tabInfo) return;
        var font = Minecraft.getInstance().font;
        String text = InfoRules.latency(info.getLatency());
        float scale = .75f;
        int right = x + width - 2;
        g.pose().pushMatrix(); g.pose().scale(scale);
        g.text(font, text, Math.round(right / scale) - font.width(text), Math.round((y + 1) / scale),
                InfoRules.latencyColor(info.getLatency()), false);
        g.pose().popMatrix(); ci.cancel();
    }
}

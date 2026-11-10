package dev.island.mixin;
import dev.island.IslandClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.DisplaySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public abstract class SingleplayerTabMixin {
    @Inject(method = "extractTabList", at = @At("HEAD"), cancellable = true)
    private void island$localTab(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (!IslandClient.CONFIG.tabInfo || !client.isLocalServer() || client.player == null || client.level == null) return;
        var tab = client.gui.hud.getTabList();
        boolean open = client.options.keyPlayerList.isDown(); tab.setVisible(open);
        if (open) {
            var board = client.level.getScoreboard();
            tab.extractRenderState(g, g.guiWidth(), board, board.getDisplayObjective(DisplaySlot.LIST));
        }
        ci.cancel();
    }
}

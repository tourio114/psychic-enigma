package dev.island.mixin;
import dev.island.LiquidGlassRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiRenderer.class)
public abstract class GlassCaptureMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepare()V"))
    private void island$capture(CallbackInfo ci) { LiquidGlassRenderer.capture(); }
    @Inject(method = "close", at = @At("HEAD"))
    private void island$close(CallbackInfo ci) { LiquidGlassRenderer.close(); }
}

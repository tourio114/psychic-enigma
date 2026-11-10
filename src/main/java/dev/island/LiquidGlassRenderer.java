package dev.island;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import dev.island.mixin.GuiGraphicsAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** Modern 26.3 RenderPearl bridge for Jacquesqwq/LiquidGlassShader's MIT optical material. */
public final class LiquidGlassRenderer {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("dynamicisland", "pipeline/liquid_glass"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexShader(Identifier.fromNamespaceAndPath("dynamicisland", "core/liquid_glass"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("dynamicisland", "core/liquid_glass"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false).build();
    private static TextureTarget snapshot;
    private static volatile boolean requested;
    private static boolean failed;
    private LiquidGlassRenderer() {}
    public static String status() { return failed ? "着色器不可用，已使用兼容外观" : "LiquidGlass · 局部模糊 / 折射 / 色散"; }
    public static void enqueue(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, double alpha) {
        if (failed || alpha <= 0 || w < 2 || h < 2) return;
        requested = true;
        int packed = ((int) Math.round(Math.clamp(alpha, 0, 1) * 255) << 24)
                | ((int) Math.round(radius / (double) Math.min(w, h) * 255) << 16) | 0x008080;
        ((GuiGraphicsAccess) g).island$state().addGuiElement(new GlassState(new Matrix3x2f(g.pose()), x, y, w, h, packed));
    }
    /** Runs before GUI meshes are sorted/built, after world rendering. Never samples the active target. */
    public static void capture() {
        if (!requested) return;
        requested = false;
        if (failed) return;
        try {
            var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            if (main.width < 1 || main.height < 1 || main.getColorTexture() == null) return;
            if (RenderSystem.getCompiledPipelineNullable(PIPELINE) == null) throw new IllegalStateException("Liquid glass pipeline unavailable");
            var format = main.getColorTexture().getFormat();
            if (snapshot == null || snapshot.width != main.width || snapshot.height != main.height
                    || snapshot.getColorTexture().getFormat() != format) {
                close();
                snapshot = new TextureTarget("Dynamic Island glass scene", main.width, main.height, format, null);
            }
            snapshot.copyColorFrom(main);
        } catch (RuntimeException e) {
            failed = true;
            System.err.println("[DynamicIsland] LiquidGlass unavailable; using compatible panel. " + e.getClass().getSimpleName());
            close();
        }
    }
    public static void close() {
        if (snapshot != null) { snapshot.destroyBuffers(); snapshot = null; }
    }
    private record GlassState(Matrix3x2f pose, int x, int y, int w, int h, int color) implements GuiElementRenderState {
        private boolean ready() { return !failed && snapshot != null; }
        @Override public RenderPipeline pipeline() { return ready() ? PIPELINE : RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() {
            return ready() ? TextureSetup.singleTexture(snapshot.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)) : TextureSetup.noTexture();
        }
        @Override public ScreenRectangle bounds() { return new ScreenRectangle(x, y, w, h).transformMaxBounds(pose); }
        @Override public ScreenRectangle scissorArea() { return bounds(); }
        @Override public void buildVertices(VertexConsumer v) {
            vertex(v, x, y, 0, 0); vertex(v, x, y + h, 0, 1);
            vertex(v, x + w, y + h, 1, 1); vertex(v, x + w, y, 1, 0);
        }
        private void vertex(VertexConsumer v, int px, int py, float u, float t) {
            var out = v.addVertexWith2DPose(pose, px, py);
            if (ready()) out.setUv(u, t).setColor(color); else out.setColor(0);
        }
    }
}

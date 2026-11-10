package dev.island;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.*;
import dev.island.mixin.GuiGraphicsAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/** Analytic rounded rectangles with coverage evaluated per physical framebuffer pixel. */
public final class SmoothShape {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("dynamicisland", "pipeline/rounded_rect"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader(Identifier.fromNamespaceAndPath("dynamicisland", "core/rounded_rect"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("dynamicisland", "core/rounded_rect"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withCull(false).build();
    public static void fill(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        add(g, x, y, w, h, radius, color, false);
    }
    public static void stroke(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        add(g, x, y, w, h, radius, color, true);
    }
    private static void add(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color, boolean stroke) {
        if (w < 1 || h < 1 || (color >>> 24) == 0) return;
        float radius = (float) Math.clamp(r / (double) Math.min(w, h), .001, .5);
        ((GuiGraphicsAccess) g).island$state().addGuiElement(new Shape(new Matrix3x2f(g.pose()), x, y, w, h,
                stroke ? -radius : radius, color));
    }
    private record Shape(Matrix3x2f pose, int x, int y, int w, int h, float radius, int color) implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return PIPELINE; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle bounds() { return new ScreenRectangle(x, y, w, h).transformMaxBounds(pose); }
        @Override public ScreenRectangle scissorArea() { return bounds(); }
        @Override public void buildVertices(VertexConsumer v) {
            vertex(v, x, y, 0, 0); vertex(v, x, y + h, 0, 1);
            vertex(v, x + w, y + h, 1, 1); vertex(v, x + w, y, 1, 0);
        }
        private void vertex(VertexConsumer v, int px, int py, float u, float t) {
            Vector2f p = pose.transformPosition(px, py, new Vector2f());
            // Z transports a constant radius/style. The vertex shader restores GUI Z to zero.
            v.addVertex(p.x, p.y, radius).setUv(u, t).setColor(color);
        }
    }
    private SmoothShape() {}
}

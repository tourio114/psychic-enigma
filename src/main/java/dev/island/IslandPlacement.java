package dev.island;

/** Scaled-pixel placement shared by HUD, drag input and numeric positioning. */
public final class IslandPlacement {
    public record Box(int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && py >= y && px < x + width && py < y + height; }
    }
    public static Box box(double anchorX, double anchorY, int screenW, int screenH, int w, int h) {
        w = Math.min(w, screenW); h = Math.min(h, screenH);
        return new Box(Math.clamp((int) Math.round(anchorX * screenW) - w / 2, 0, Math.max(0, screenW - w)),
                Math.clamp((int) Math.round(anchorY * screenH), 0, Math.max(0, screenH - h)), w, h);
    }
    public static double anchorX(double left, int panelW, int screenW) {
        return Math.clamp((left + panelW / 2.0) / Math.max(1, screenW), 0, 1);
    }
    public static double anchorY(double top, int screenH) { return Math.clamp(top / Math.max(1, screenH), 0, 1); }
    private IslandPlacement() {}
}

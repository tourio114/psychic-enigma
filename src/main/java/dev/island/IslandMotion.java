package dev.island;

/** Frame-rate independent HUD motion. Kept independent of the game for deterministic checks. */
public final class IslandMotion {
    public double width = 112, height = 28, appearance, transition = 1, health = 1, pulse;
    private double widthVelocity, heightVelocity;
    private String identity = "";

    public void enter() { appearance = 0; }
    public void hit() { pulse = 1; }
    public boolean change(String next) {
        if (identity.equals(next)) return false;
        identity = next; transition = 0; return true;
    }
    public void step(double seconds, boolean expanded, double healthRatio, boolean animations) {
        double dt = Double.isFinite(seconds) ? Math.clamp(seconds, 0, .1) : 0;
        double targetWidth = expanded ? 236 : 112, targetHeight = expanded ? 54 : 28;
        double ratio = Double.isFinite(healthRatio) ? Math.clamp(healthRatio, 0, 1) : 1;
        if (!animations) {
            width = targetWidth; height = targetHeight; health = ratio;
            widthVelocity = heightVelocity = pulse = 0; appearance = transition = 1; return;
        }
        // Exact critically damped spring solution; no overshoot and no FPS-dependent Euler drift.
        double decay = Math.exp(-18 * dt);
        double wd = width - targetWidth, hd = height - targetHeight;
        double wj = widthVelocity + 18 * wd, hj = heightVelocity + 18 * hd;
        width = targetWidth + (wd + wj * dt) * decay;
        height = targetHeight + (hd + hj * dt) * decay;
        widthVelocity = (widthVelocity - 18 * wj * dt) * decay;
        heightVelocity = (heightVelocity - 18 * hj * dt) * decay;
        appearance = Math.min(1, appearance + dt / .28);
        transition = Math.min(1, transition + dt / .22);
        health += (ratio - health) * (1 - Math.exp(-10 * dt));
        pulse *= Math.exp(-7 * dt);
    }
    public static double smooth(double t) { t = Math.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
}

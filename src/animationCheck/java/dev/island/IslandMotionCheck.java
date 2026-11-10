package dev.island;

/** Deterministic behavioral checks, run by Gradle check and in CI. */
public final class IslandMotionCheck {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static IslandMotion advance(int fps) {
        IslandMotion m = new IslandMotion();
        for (int i = 0; i < fps; i++) m.step(1.0 / fps, true, .2, true);
        return m;
    }
    public static void main(String[] args) {
        IslandMotion low = advance(30), high = advance(144);
        check(Math.abs(low.width - high.width) < 1e-7, "spring must be frame-rate independent");
        check(Math.abs(low.health - high.health) < 1e-7, "health must be frame-rate independent");
        IslandMotion m = new IslandMotion();
        double last = m.width;
        for (int i = 0; i < 120; i++) {
            m.step(1.0 / 60, true, .3, true);
            check(m.width >= last && m.width <= 236, "expansion must not overshoot");
            last = m.width;
        }
        for (int i = 0; i < 120; i++) m.step(1.0 / 60, false, 0, true);
        check(Math.abs(m.width - 112) < .01 && m.health < .001, "collapse and damage must converge");
        m.hit(); m.step(.1, true, 1, true);
        check(m.pulse > 0 && m.pulse < 1, "hit highlight must decay");
        check(m.change("combat:1") && !m.change("combat:1"), "health updates must not restart text transitions");
        m.step(0, true, .5, false);
        check(m.width == 236 && m.height == 54 && m.transition == 1 && m.appearance == 1
                && m.health == .5 && m.pulse == 0, "disabled animations must snap without motion");
        m.enter(); m.step(.1, true, Double.NaN, true);
        check(m.appearance > 0 && m.appearance < 1 && Double.isFinite(m.health), "entry fade and invalid health");
        System.out.println("Animation checks passed: FPS independence, bounds, transitions, health, pulse and reduced motion.");
    }
}

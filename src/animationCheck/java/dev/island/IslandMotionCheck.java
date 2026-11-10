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
        for (int[] screen : new int[][]{{320, 240}, {480, 270}, {1920, 1080}}) {
            var box = IslandPlacement.box(.5, .035, screen[0], screen[1], 112, 28);
            check(box.contains(box.x() + 10, box.y() + 5), "HUD drag hit area");
            check(!box.contains(box.x() - 1, box.y()), "outside HUD must not capture drag");
            var moved = IslandPlacement.box(IslandPlacement.anchorX(57, 112, screen[0]),
                    IslandPlacement.anchorY(73, screen[1]), screen[0], screen[1], 112, 28);
            check(moved.x() == 57 && moved.y() == 73, "drag coordinates must round trip at each GUI scale");
            var edge = IslandPlacement.box(1, 1, screen[0], screen[1], 236, 54);
            check(edge.x() + edge.width() <= screen[0] && edge.y() + edge.height() <= screen[1], "clamp to visible screen");
        }
        check(InfoRules.duration(Float.NaN) == 5 && InfoRules.duration(Float.POSITIVE_INFINITY) == 5,
                "invalid combat durations must use a safe default");
        check(InfoRules.duration(-10) == 1 && InfoRules.duration(100) == 30, "duration bounds");
        check(InfoRules.nanos(30) == 30_000_000_000L && InfoRules.nanos(1) == 1_000_000_000L, "duration units must not overflow");
        check(InfoRules.latency(-1).equals("--") && InfoRules.latency(42).equals("42 ms")
                && InfoRules.latency(99999).equals("9999+ ms"), "latency formatting");
        System.out.println("Info checks passed: duration defaults, bounds, nanosecond timing and latency formatting.");
        System.out.println("Placement checks passed: scaled coordinates, drag hit area, anchor round trip and edge limits.");
        System.out.println("Animation checks passed: FPS independence, bounds, transitions, health, pulse and reduced motion.");
    }
}

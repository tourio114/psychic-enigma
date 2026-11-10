package dev.island;
/** Bounded configuration and display formatting shared by both network sides. */
public final class InfoRules {
    public static float duration(float seconds) { return Float.isFinite(seconds) ? Math.clamp(seconds, 1, 30) : 5; }
    public static long nanos(float seconds) { return (long) (duration(seconds) * 1_000_000_000.0); }
    public static String latency(int milliseconds) {
        return milliseconds < 0 ? "--" : milliseconds > 9999 ? "9999+ ms" : milliseconds + " ms";
    }
    public static int latencyColor(int ms) { return ms < 0 ? 0xFF9EABC5 : ms < 100 ? 0xFF8EE5B0 : ms < 250 ? 0xFFF0D582 : 0xFFFF9C9C; }
    private InfoRules() {}
}

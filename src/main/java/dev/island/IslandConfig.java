package dev.island;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public final class IslandConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dynamicisland.json");
    public double x = 0.5, y = 0.035;
    public boolean seconds = false;
    public float opacity = 0.62f;
    public boolean glass = true;
    public boolean animations = true;
    public String api = "http://localhost:3000";
    public float volume = 0.65f;

    public static IslandConfig load() {
        try {
            if (Files.exists(FILE)) {
                IslandConfig c = JSON.fromJson(Files.readString(FILE), IslandConfig.class);
                if (c != null) {
                    c.x = finite(c.x, .5); c.y = finite(c.y, .035);
                    c.volume = (float) finite(c.volume, .65);
                    c.opacity = (float) finite(c.opacity, .62);
                    if (c.api == null) c.api = "http://localhost:3000";
                    return c;
                }
            }
        } catch (Exception e) { System.err.println("[DynamicIsland] 无法读取配置，使用默认值"); }
        return new IslandConfig();
    }
    private static double finite(double n, double fallback) {
        return Double.isFinite(n) ? Math.clamp(n, 0, 1) : fallback;
    }
    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("dynamicisland.json.tmp");
            Files.writeString(tmp, JSON.toJson(this));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { throw new IllegalStateException("无法保存灵动岛配置", e); }
    }
}

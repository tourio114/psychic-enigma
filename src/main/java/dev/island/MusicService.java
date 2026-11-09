package dev.island;

import com.google.gson.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import net.fabricmc.loader.api.FabricLoader;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class MusicService {
    public record Song(long id, String name, String artist) {}
    private final IslandConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ScheduledExecutorService worker = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "island-api"); t.setDaemon(true); return t;
    });
    private final Path session = FabricLoader.getInstance().getConfigDir().resolve("dynamicisland.session");
    private String cookie = "", cookieOrigin = "";
    private final AtomicInteger loginVersion = new AtomicInteger();
    private ScheduledFuture<?> polling;
    public volatile BitMatrix qr;
    public volatile String status = "未登录";
    public final Mp3Audio audio = new Mp3Audio();

    public MusicService(IslandConfig config) {
        this.config = config;
        try {
            JsonObject s = JsonParser.parseString(Files.readString(session)).getAsJsonObject();
            cookie = s.get("cookie").getAsString(); cookieOrigin = s.get("origin").getAsString();
            status = "已载入登录凭证（待验证）";
        } catch (Exception ignored) {}
    }
    private String base() {
        String s = config.api.strip().replaceAll("/+$", "");
        URI uri = URI.create(s);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("请填写有效的 API HTTP(S) 地址");
        return s;
    }
    private synchronized String credential(String origin) { return origin.equals(cookieOrigin) ? cookie : ""; }
    private JsonObject call(String origin, String path, Map<String, String> args) throws Exception {
        Map<String, String> form = new LinkedHashMap<>(args);
        String auth = credential(origin);
        if (!auth.isEmpty()) form.put("cookie", auth);
        form.put("timestamp", Long.toString(System.currentTimeMillis()));
        StringJoiner body = new StringJoiner("&");
        form.forEach((k, v) -> body.add(URLEncoder.encode(k, java.nio.charset.StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8)));
        var request = HttpRequest.newBuilder(URI.create(origin + path)).timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("API HTTP " + response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        int code = json.has("code") ? json.get("code").getAsInt() : 200;
        if (code == 301 || code == 302) throw new IllegalStateException("登录已失效，请重新扫码");
        if (code >= 400 && !(code >= 800 && code <= 803)) throw new IllegalStateException("API 返回错误 " + code);
        return json;
    }
    public CompletableFuture<List<Song>> search(String text) {
        String origin = base();
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject json = call(origin, "/cloudsearch", Map.of("keywords", text, "limit", "5", "type", "1"));
                List<Song> results = new ArrayList<>();
                JsonObject result = json.getAsJsonObject("result");
                if (result != null && result.has("songs")) for (JsonElement e : result.getAsJsonArray("songs")) {
                    JsonObject song = e.getAsJsonObject();
                    StringJoiner artists = new StringJoiner(" / ");
                    for (JsonElement a : song.getAsJsonArray("ar")) artists.add(a.getAsJsonObject().get("name").getAsString());
                    results.add(new Song(song.get("id").getAsLong(), song.get("name").getAsString(), artists.toString()));
                }
                return List.copyOf(results);
            } catch (Exception e) { throw new CompletionException(e); }
        }, worker);
    }
    private final AtomicInteger playVersion = new AtomicInteger();
    public void play(Song song) {
        int generation = playVersion.incrementAndGet();
        audio.stop(); status = "正在获取播放地址…";
        worker.execute(() -> {
            try {
                String origin = base();
                JsonObject j = call(origin, "/song/url/v1", Map.of("id", Long.toString(song.id), "level", "standard", "encodeType", "mp3"));
                JsonObject data = j.getAsJsonArray("data").get(0).getAsJsonObject();
                if (data.get("url").isJsonNull()) throw new IllegalStateException("无播放权限：请确认登录、VIP 或地区限制");
                if (data.has("freeTrialInfo") && !data.get("freeTrialInfo").isJsonNull())
                    throw new IllegalStateException("该歌曲仅返回试听地址，请确认账号 VIP 权限");
                synchronized (this) {
                    if (generation != playVersion.get()) return;
                    audio.play(URI.create(data.get("url").getAsString()), song.name + " · " + song.artist, config.volume);
                    status = "正在播放：" + song.name;
                }
            } catch (Exception e) { if (generation == playVersion.get()) status = "播放失败：" + safe(e); }
        });
    }
    public synchronized void stop() { playVersion.incrementAndGet(); audio.stop(); }
    public synchronized void login() {
        cancelLogin(); int generation = loginVersion.get();
        status = "正在生成登录二维码…";
        worker.execute(() -> {
            try {
                String origin = base();
                String key = call(origin, "/login/qr/key", Map.of("type", "1")).getAsJsonObject("data").get("unikey").getAsString();
                String url = call(origin, "/login/qr/create", Map.of("key", key, "qrimg", "false", "type", "1"))
                        .getAsJsonObject("data").get("qrurl").getAsString();
                BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 1, 1);
                synchronized (this) {
                    if (generation != loginVersion.get()) return;
                    qr = matrix; status = "使用网易云音乐 App 扫码并确认";
                    long deadline = System.nanoTime() + 150_000_000_000L;
                    polling = worker.scheduleWithFixedDelay(() -> checkLogin(origin, key, generation, deadline), 2, 2, TimeUnit.SECONDS);
                }
            } catch (Exception e) { if (generation == loginVersion.get()) status = "登录失败：" + safe(e); }
        });
    }
    private void checkLogin(String origin, String key, int generation, long deadline) {
        try {
            if (generation != loginVersion.get()) return;
            if (System.nanoTime() > deadline) throw new IllegalStateException("二维码已过期，请重新生成");
            JsonObject j = call(origin, "/login/qr/check", Map.of("key", key, "type", "1"));
            int code = j.get("code").getAsInt();
            synchronized (this) {
                if (generation != loginVersion.get()) return;
                if (code == 800) throw new IllegalStateException("二维码已过期，请重新生成");
                if (code == 802) status = "已扫码，请在手机上确认";
                if (code == 803) {
                    String auth = j.get("cookie").getAsString();
                    if (auth.isBlank()) throw new IllegalStateException("API 未返回登录凭证");
                    cookie = auth; cookieOrigin = origin;
                    JsonObject saved = new JsonObject(); saved.addProperty("origin", origin); saved.addProperty("cookie", auth);
                    if (!Files.exists(session)) {
                        try { Files.createFile(session, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))); }
                        catch (UnsupportedOperationException e) { Files.createFile(session); }
                    }
                    Files.writeString(session, saved.toString());
                    try { Files.setPosixFilePermissions(session, PosixFilePermissions.fromString("rw-------")); }
                    catch (UnsupportedOperationException ignored) {}
                    status = "登录成功，可使用账号已有的 VIP 权限"; cancelLogin();
                }
            }
        } catch (Exception e) {
            synchronized (this) {
                if (generation == loginVersion.get()) { status = "登录失败：" + safe(e); cancelLogin(); }
            }
        }
    }
    public synchronized void cancelLogin() {
        loginVersion.incrementAndGet(); qr = null;
        if (polling != null) { polling.cancel(false); polling = null; }
    }
    public synchronized void logout() {
        cancelLogin(); stop(); cookie = ""; cookieOrigin = "";
        try { Files.deleteIfExists(session); status = "已退出并清除本地凭证"; }
        catch (Exception e) { status = "退出成功，但无法删除凭证文件，请手动删除 config/dynamicisland.session"; }
    }
    public void close() { cancelLogin(); stop(); worker.shutdownNow(); audio.close(); }
    public static String safe(Throwable e) {
        Throwable t = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
        // Never show server response bodies or URLs: they can contain credentials.
        return t instanceof IllegalStateException || t instanceof IllegalArgumentException ? t.getMessage() : "网络、音频格式或设备错误（" + t.getClass().getSimpleName() + "）";
    }
}

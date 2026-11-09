package dev.island;

import javazoom.jl.decoder.*;
import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Decode MP3 on a daemon thread; JavaSound handles PCM output, independent of the render thread. */
public final class Mp3Audio {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "island-audio"); t.setDaemon(true); return t;
    });
    private final AtomicInteger generation = new AtomicInteger();
    private volatile InputStream input;
    private volatile SourceDataLine line;
    private volatile boolean paused;
    public volatile boolean playing;
    public volatile String title = "", error = "";
    public volatile float volume = .65f;
    public void play(URI uri, String title, float volume) {
        stop(); int token = generation.get();
        this.title = title; this.volume = volume; error = "";
        worker.execute(() -> decode(uri, token));
    }
    private void decode(URI uri, int token) {
        SourceDataLine localLine = null;
        InputStream localInput = null;
        try {
            if (token != generation.get()) return;
            if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) throw new IOException();
            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
            localInput = new BufferedInputStream(connection.getInputStream());
            synchronized (this) {
                if (token != generation.get()) return;
                input = localInput;
            }
            Bitstream bits = new Bitstream(localInput); Decoder decoder = new Decoder(); Header header;
            while (token == generation.get() && (header = bits.readFrame()) != null) {
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bits);
                if (localLine == null) {
                    AudioFormat format = new AudioFormat(samples.getSampleFrequency(), 16, samples.getChannelCount(), true, false);
                    localLine = AudioSystem.getSourceDataLine(format); localLine.open(format); localLine.start();
                    synchronized (this) {
                        if (token != generation.get()) return;
                        line = localLine; playing = true;
                    }
                }
                while (paused && token == generation.get()) Thread.sleep(20);
                if (token != generation.get()) break;
                short[] pcm = samples.getBuffer(); byte[] bytes = new byte[samples.getBufferLength() * 2];
                for (int i = 0; i < samples.getBufferLength(); i++) {
                    int s = (int) (pcm[i] * Math.clamp(volume, 0, 1));
                    bytes[i * 2] = (byte) s; bytes[i * 2 + 1] = (byte) (s >> 8);
                }
                localLine.write(bytes, 0, bytes.length); bits.closeFrame();
            }
            if (localLine != null && token == generation.get()) localLine.drain();
        } catch (Exception e) { if (token == generation.get()) error = "无法播放：MP3 格式、网络或音频设备错误"; }
        finally {
            if (localLine != null) localLine.close();
            if (localInput != null) try { localInput.close(); } catch (IOException ignored) {}
            synchronized (this) {
                if (token == generation.get()) { playing = false; line = null; input = null; }
            }
        }
    }
    public synchronized void togglePause() {
        paused = !paused;
        if (line != null) { if (paused) line.stop(); else line.start(); }
    }
    public boolean paused() { return paused; }
    public synchronized void stop() {
        generation.incrementAndGet(); playing = false; paused = false;
        if (line != null) { line.stop(); line.flush(); line.close(); line = null; }
        if (input != null) { try { input.close(); } catch (IOException ignored) {} input = null; }
    }
    public void close() { stop(); worker.shutdownNow(); }
}

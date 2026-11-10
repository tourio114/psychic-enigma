package dev.island;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

public final class IslandCommon implements ModInitializer {
    private record Watch(ServerPlayer player, LivingEntity target, long until) {}
    private final Map<UUID, Watch> watches = new HashMap<>();
    private final Map<UUID, Float> durations = new HashMap<>();
    private int statusTicks;
    @Override public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(HealthPayload.TYPE, HealthPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerInfoPayload.TYPE, ServerInfoPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DurationPayload.TYPE, DurationPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DurationPayload.TYPE, (payload, context) -> context.server().execute(() ->
                durations.put(context.player().getUUID(), InfoRules.duration(payload.seconds()))));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            durations.remove(handler.player.getUUID()); watches.remove(handler.player.getUUID());
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            watches.clear(); durations.clear(); statusTicks = 0;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player instanceof ServerPlayer sp && entity instanceof LivingEntity living
                    && !sp.isSpectator() && ServerPlayNetworking.canSend(sp, HealthPayload.TYPE)) {
                watches.put(sp.getUUID(), new Watch(sp, living, System.nanoTime() + InfoRules.nanos(durations.getOrDefault(sp.getUUID(), 5f))));
            }
            return InteractionResult.PASS;
        });
        // End-of-tick runs after attack damage is applied. Subsequent ticks track healing and death too.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++statusTicks >= 20) {
                statusTicks = 0;
                var players = server.getPlayerList();
                var status = new ServerInfoPayload(server.getAverageTickTimeNanos() / 1_000_000f,
                        server.tickRateManager().isFrozen() ? 0 : server.tickRateManager().tickrate(),
                        players.getPlayerCount(), players.getMaxPlayers());
                for (ServerPlayer player : players.getPlayers()) if (ServerPlayNetworking.canSend(player, ServerInfoPayload.TYPE))
                    ServerPlayNetworking.send(player, status);
            }
            var it = watches.values().iterator();
            while (it.hasNext()) {
                Watch w = it.next();
                if (System.nanoTime() > w.until || w.player.hasDisconnected()
                        || w.player.level() != w.target.level()) { it.remove(); continue; }
                ServerPlayNetworking.send(w.player, new HealthPayload(w.target.getId(),
                        w.target.getHealth(), w.target.getMaxHealth()));
                if (w.target.isRemoved() || !w.target.isAlive()) it.remove();
            }
        });
    }
}

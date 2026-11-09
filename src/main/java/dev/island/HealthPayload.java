package dev.island;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HealthPayload(int entityId, float health, float maxHealth) implements CustomPacketPayload {
    public static final Type<HealthPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dynamicisland", "health"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HealthPayload> CODEC = new StreamCodec<>() {
        public HealthPayload decode(RegistryFriendlyByteBuf b) { return new HealthPayload(b.readVarInt(), b.readFloat(), b.readFloat()); }
        public void encode(RegistryFriendlyByteBuf b, HealthPayload p) { b.writeVarInt(p.entityId); b.writeFloat(p.health); b.writeFloat(p.maxHealth); }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

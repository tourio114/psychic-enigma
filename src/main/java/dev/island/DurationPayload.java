package dev.island;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record DurationPayload(float seconds) implements CustomPacketPayload {
    public static final Type<DurationPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dynamicisland", "combat_duration"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DurationPayload> CODEC = new StreamCodec<>() {
        public DurationPayload decode(RegistryFriendlyByteBuf b) { return new DurationPayload(InfoRules.duration(b.readFloat())); }
        public void encode(RegistryFriendlyByteBuf b, DurationPayload p) { b.writeFloat(InfoRules.duration(p.seconds)); }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

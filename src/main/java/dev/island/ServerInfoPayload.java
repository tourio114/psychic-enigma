package dev.island;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record ServerInfoPayload(float mspt, float targetTps, int online, int maximum) implements CustomPacketPayload {
    public static final Type<ServerInfoPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("dynamicisland", "server_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerInfoPayload> CODEC = new StreamCodec<>() {
        public ServerInfoPayload decode(RegistryFriendlyByteBuf b) { return new ServerInfoPayload(b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt()); }
        public void encode(RegistryFriendlyByteBuf b, ServerInfoPayload p) { b.writeFloat(p.mspt); b.writeFloat(p.targetTps); b.writeVarInt(p.online); b.writeVarInt(p.maximum); }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

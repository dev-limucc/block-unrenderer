package dev.limucc.blockunrenderer.net;

import dev.limucc.blockunrenderer.BlockUnRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client opt-in handshake.
 *
 * <p>Sent to a player on join <em>only</em> when the server has explicitly opted in
 * (server config {@code allowSeeThrough = true}). Its presence is the signal that the
 * see-through / block-hiding feature is permitted on this server. Without it, the client
 * keeps the feature fully disabled in multiplayer, satisfying Modrinth's Content Rules
 * (section 3: x-ray / seeing through opaque blocks requires a server-side opt-in).
 *
 * <p>An empty payload — the message itself is the signal, so it carries no fields.
 */
public record OptInPayload() implements CustomPacketPayload {

    public static final OptInPayload INSTANCE = new OptInPayload();

    public static final CustomPacketPayload.Type<OptInPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BlockUnRenderer.MOD_ID, "opt_in"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OptInPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<OptInPayload> type() {
        return TYPE;
    }
}

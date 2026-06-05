package dev.limucc.blockunrenderer;

import dev.limucc.blockunrenderer.net.OptInPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockUnRenderer implements ModInitializer {

    public static final String MOD_ID = "block_unrenderer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register the opt-in handshake codec on both physical sides
        // (a server encodes it to send; a client decodes it to receive).
        PayloadTypeRegistry.clientboundPlay().register(OptInPayload.TYPE, OptInPayload.CODEC);

        // Server-side opt-in (Modrinth Content Rules section 3: x-ray / seeing through opaque
        // blocks must require a server opt-in). Default OFF — an operator must set
        // allowSeeThrough=true in config/block-unrenderer-server.json to permit the feature.
        ServerOptInConfig.load();

        // On join, only servers that have opted in tell the client the feature is allowed here.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerOptInConfig.get().allowSeeThrough) {
                ServerPlayNetworking.send(handler.player, OptInPayload.INSTANCE);
            }
        });

        LOGGER.info("Block UN-renderer loaded. Server see-through opt-in: {}",
                ServerOptInConfig.get().allowSeeThrough);
    }
}

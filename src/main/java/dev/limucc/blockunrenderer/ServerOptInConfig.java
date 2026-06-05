package dev.limucc.blockunrenderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side opt-in for the see-through feature (Modrinth Content Rules section 3).
 *
 * <p>Written to {@code config/block-unrenderer-server.json}. <b>Default {@code false}</b>: the
 * block-hiding / see-through ability is disabled for all clients connecting to this server unless
 * an operator deliberately turns it on. A vanilla server (which has no way to send the opt-in
 * handshake) therefore never enables the feature — exactly what the rule requires.
 */
public final class ServerOptInConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("block-unrenderer-server.json");

    /** When true, this server permits connecting clients to use the see-through feature. */
    public boolean allowSeeThrough = false;

    private static ServerOptInConfig instance;

    public static ServerOptInConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                ServerOptInConfig cfg = GSON.fromJson(Files.readString(PATH), ServerOptInConfig.class);
                instance = (cfg != null) ? cfg : new ServerOptInConfig();
            } else {
                instance = new ServerOptInConfig();
                save(); // write the default so admins can find and edit it
            }
        } catch (Exception e) {
            BlockUnRenderer.LOGGER.warn("[Block UN-renderer] Failed to read server config, using defaults", e);
            instance = new ServerOptInConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(instance != null ? instance : new ServerOptInConfig()));
        } catch (IOException e) {
            BlockUnRenderer.LOGGER.warn("[Block UN-renderer] Failed to write server config", e);
        }
    }
}

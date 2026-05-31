package dev.limucc.blockunrenderer.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.limucc.blockunrenderer.BlockUnRenderer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("block_unrenderer.json");

    private static ModConfig instance = new ModConfig();

    public static ModConfig get() { return instance; }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) { save(); return; }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            ModConfig loaded = GSON.fromJson(r, ModConfig.class);
            instance = (loaded != null) ? loaded : new ModConfig();
        } catch (IOException e) {
            BlockUnRenderer.LOGGER.error("Failed to load config.", e);
            instance = new ModConfig();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            BlockUnRenderer.LOGGER.error("Failed to save config.", e);
        }
    }
}

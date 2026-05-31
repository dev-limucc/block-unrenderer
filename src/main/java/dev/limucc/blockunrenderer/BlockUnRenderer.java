package dev.limucc.blockunrenderer;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockUnRenderer implements ModInitializer {

    public static final String MOD_ID = "block_unrenderer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Block UN-renderer loaded.");
    }
}

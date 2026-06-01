package dev.limucc.blockunrenderer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.limucc.blockunrenderer.client.gui.BlockManagerScreen;

/**
 * Opens the custom Block Manager from ModMenu — search box, scrollable list (blocks,
 * block entities, liquids) with icons, click-to-add/remove, and the toggles as buttons.
 * All text is visible (vanilla widgets).
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BlockManagerScreen::new;
    }
}

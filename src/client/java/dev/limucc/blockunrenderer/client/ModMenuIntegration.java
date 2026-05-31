package dev.limucc.blockunrenderer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.limucc.blockunrenderer.client.gui.BlockManagerScreen;

/**
 * Opens the custom block manager from ModMenu — search box, scrollable block list
 * with icons, click-to-add/remove, and the toggles as buttons. All text is visible
 * (vanilla widgets), unlike Cloth's text inputs in 26.1.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BlockManagerScreen::new;
    }
}

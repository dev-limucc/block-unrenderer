package dev.limucc.blockunrenderer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.limucc.blockunrenderer.client.gui.BlockPickerScreen;

/**
 * Opens our custom block-picker screen from ModMenu — a hand-built Screen with
 * a search bar, 3D block icons, and click-to-add/remove. (No Cloth Config.)
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BlockPickerScreen::new;
    }
}

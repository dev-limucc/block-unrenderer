package dev.limucc.blockunrenderer.client.mixin.xaero;

import dev.limucc.blockunrenderer.client.render.HideState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Map-mod guard: while Xaero's Minimap samples the world to build its map, report the
 * real (unhidden) world so hidden blocks aren't stripped from the minimap.
 *
 * This is a soft, best-effort guard:
 *  - {@link Pseudo} + {@code targets} lets us reference Xaero (not on our classpath, not remapped).
 *  - {@code required:false} (config) + {@code require = 0} (injectors) mean it is completely
 *    harmless if Xaero is absent OR if this Xaero version uses a different method — it simply
 *    does nothing, and {@link HideState#isMapSampling()}'s thread-name fallback still applies.
 *
 * NOTE: {@code onRender} is the confirmed entry point Xaero uses each frame
 * (see MinimapProcessor.onRender in crash logs). If a future Xaero version samples elsewhere,
 * update the target after decompiling the installed Xaeros_Minimap jar.
 */
@Pseudo
@Mixin(targets = "xaero.common.minimap.MinimapProcessor")
public class XaeroMinimapProcessorMixin {

    @Inject(method = "onRender", at = @At("HEAD"), require = 0, remap = false)
    private void bur$mapSampleBegin(CallbackInfo ci) {
        HideState.pushMapSampling();
    }

    @Inject(method = "onRender", at = @At("RETURN"), require = 0, remap = false)
    private void bur$mapSampleEnd(CallbackInfo ci) {
        HideState.popMapSampling();
    }
}

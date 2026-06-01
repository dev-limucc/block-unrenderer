package dev.limucc.blockunrenderer.client.mixin.xaero;

import dev.limucc.blockunrenderer.client.render.HideState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Map-mod guard for Xaero's WORLD MAP.
 *
 * The world map samples the world in {@code MapWriter.onRender(...)} → writeChunk → loadPixel,
 * reading {@code getRenderShape()} / {@code getMapColor()} like the minimap writer. Same guard:
 * report the real world while it samples so hidden blocks stay on the saved map.
 *
 * Soft-mixin (non-required, {@code require = 0}); harmless when Xaero's World Map is absent.
 */
@Pseudo
@Mixin(targets = "xaero.map.MapWriter", remap = false)
public class XaeroWorldMapWriterMixin {

    @Inject(method = "onRender", at = @At("HEAD"), require = 0, remap = false)
    private void bur$mapSampleBegin(CallbackInfo ci) {
        HideState.pushMapSampling();
    }

    @Inject(method = "onRender", at = @At("RETURN"), require = 0, remap = false)
    private void bur$mapSampleEnd(CallbackInfo ci) {
        HideState.popMapSampling();
    }
}

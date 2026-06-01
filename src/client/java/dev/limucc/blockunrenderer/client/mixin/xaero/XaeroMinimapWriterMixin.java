package dev.limucc.blockunrenderer.client.mixin.xaero;

import dev.limucc.blockunrenderer.client.render.HideState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Map-mod guard for Xaero's MINIMAP.
 *
 * Xaero builds the minimap in {@code MinimapWriter.onRender()} → writeChunk → loadBlockColor /
 * findBlock, which read {@code BlockState.getRenderShape()}, {@code getMapColor()} and
 * {@code getLightEmission()} — exactly the methods our render mixins modify. So while this runs we
 * must report the REAL world, or hidden blocks get stripped from / recoloured on the minimap.
 *
 * (The previous guard wrapped {@code MinimapProcessor.onRender}, which does NOT sample blocks —
 * that's why hidden blocks still changed the map.)
 *
 * Soft-mixin: {@link Pseudo} + {@code targets} + non-required config + {@code require = 0} → it's a
 * no-op if Xaero is absent or this method moves in a future version (the thread-name fallback in
 * {@link HideState#isMapSampling()} still covers Xaero's worker threads).
 */
@Pseudo
@Mixin(targets = "xaero.common.minimap.write.MinimapWriter", remap = false)
public class XaeroMinimapWriterMixin {

    @Inject(method = "onRender", at = @At("HEAD"), require = 0, remap = false)
    private void bur$mapSampleBegin(CallbackInfo ci) {
        HideState.pushMapSampling();
    }

    @Inject(method = "onRender", at = @At("RETURN"), require = 0, remap = false)
    private void bur$mapSampleEnd(CallbackInfo ci) {
        HideState.popMapSampling();
    }
}

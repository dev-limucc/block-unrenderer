package dev.limucc.blockunrenderer.client.mixin;

import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NIGHT_VISION mode: apply permanent night vision (classic tinted look).
 * FULLBRIGHT is handled separately by LightmapClearMixin (uniform white lightmap).
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
    private void bur$nightVision(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (HideState.isLightActive() && HideState.lightMode() == ModConfig.LightMode.NIGHT_VISION) {
            renderState.nightVisionEffectIntensity = 1.0F;
            renderState.darknessEffectScale = 0.0F;
            renderState.bossOverlayWorldDarkening = 0.0F;
        }
    }
}

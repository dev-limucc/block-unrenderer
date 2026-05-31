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
 * Lighting boost via the lightmap's night-vision lever (the field that actually
 * beats the shader's gamma clamp). Applied for BOTH modes so the scene is bright:
 *   FULLBRIGHT   → night vision + neutral white tint (+ LightmapClearMixin tries to
 *                  white the texture so even covered light-0 faces go full bright)
 *   NIGHT_VISION → night vision with the classic tint
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
    private void bur$light(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (!HideState.isLightActive()) return;

        renderState.nightVisionEffectIntensity = 1.0F;
        renderState.darknessEffectScale = 0.0F;
        renderState.bossOverlayWorldDarkening = 0.0F;
        renderState.brightness = 1.0F;

        if (HideState.lightMode() == ModConfig.LightMode.FULLBRIGHT) {
            renderState.nightVisionColor = LightmapRenderStateExtractor.WHITE; // neutral, no green tint
        }
    }
}

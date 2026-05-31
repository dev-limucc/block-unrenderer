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
 * Lighting boost while hiding.
 *
 * The lightmap shader clamps `brightness` (gamma), so it alone can't brighten dark
 * areas. nightVisionEffectIntensity is the shader's real "see in the dark" lever —
 * 1.0 lifts even light level 0 to fully visible. We use it for both modes:
 *   FULLBRIGHT   → neutral white tint (clean, fullbright look)
 *   NIGHT_VISION → default tint (classic night-vision look)
 *
 * Works in vanilla/Sodium. Under Iris shaders the shader owns lighting.
 * Zero cost when off (one boolean check).
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
    private void bur$light(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (!HideState.isLightActive()) return;

        renderState.darknessEffectScale = 0.0F;
        renderState.bossOverlayWorldDarkening = 0.0F;

        if (HideState.lightMode() == ModConfig.LightMode.FULLBRIGHT) {
            // Overshoot night-vision so even light-level-0 areas (the covered blocks
            // under hidden ones) saturate to full white instead of staying gray.
            // The lightmap texture is RGBA8, so the shader clamps the result — the
            // net effect is a uniform, fully-bright lightmap.
            renderState.nightVisionEffectIntensity = 10.0F;
            renderState.brightness = 1.0F;
            renderState.nightVisionColor = LightmapRenderStateExtractor.WHITE; // neutral, no green tint
        } else { // NIGHT_VISION — classic look
            renderState.nightVisionEffectIntensity = 1.0F;
        }
    }
}

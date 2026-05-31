package dev.limucc.blockunrenderer.client.mixin;

import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Strong fullbright while hiding (Fix Lighting on).
 *
 * LightmapRenderStateExtractor.extract() computes renderState.brightness from the
 * gamma option (capped at 1.0 by vanilla — which wasn't enough). We override the
 * public brightness field at TAIL to a large value, which the lightmap shader
 * lifts to (clamped) full brightness, so exposed areas under hidden blocks are
 * clearly visible.
 *
 * Works for vanilla and Sodium (both sample this lightmap). Iris shaders compute
 * their own lighting and ignore the lightmap, so under shaders use the shader's
 * brightness/night-vision. Zero cost when off (one boolean check).
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
    private void bur$fullbright(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (HideState.isFullbright()) {
            renderState.brightness = 16.0F;
        }
    }
}

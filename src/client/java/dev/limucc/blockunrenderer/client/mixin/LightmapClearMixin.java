package dev.limucc.blockunrenderer.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FULLBRIGHT that also lights covered under-blocks.
 *
 * A face samples the light VALUE at its neighbour cell; covered blocks have light
 * level 0 there, so even a brightened lightmap curve leaves them grayer than
 * skylit terrain. Fix: after the lightmap is built, overwrite the whole 16×16
 * lightmap texture with solid white (clearColorTexture(texture, 0xFFFFFFFF)). Then
 * EVERY light coordinate — including (0,0) on covered faces — samples white, so
 * they render exactly as bright as everything else. Both vanilla and Sodium sample
 * this texture. (Iris shaders own their lighting and ignore it.)
 *
 * Cost: one 16×16 GPU clear per frame while FULLBRIGHT is active — negligible.
 */
@Mixin(Lightmap.class)
public class LightmapClearMixin {

    @Shadow @Final private GpuTexture texture;

    // Runs once per frame (not per block), so @Inject's CallbackInfo cost is irrelevant here.
    @Inject(method = "render(Lnet/minecraft/client/renderer/state/LightmapRenderState;)V", at = @At("TAIL"))
    private void bur$whiteLightmap(LightmapRenderState renderState, CallbackInfo ci) {
        if (HideState.isLightActive()) { // isLightActive() already implies active + FULLBRIGHT
            RenderSystem.getDevice().createCommandEncoder()
                    .clearColorTexture(this.texture, 0xFFFFFFFF); // white
        }
    }
}

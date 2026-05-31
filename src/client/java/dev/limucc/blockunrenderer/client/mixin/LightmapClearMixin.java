package dev.limucc.blockunrenderer.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.limucc.blockunrenderer.client.config.ModConfig;
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
 * TRUE fullbright that also lights the covered blocks under hidden ones.
 *
 * The problem: a face's brightness samples the light VALUE at its neighbour cell.
 * Blocks that were covered have light level 0 there, so even a brightened lightmap
 * curve leaves them grayer than skylit terrain (level-0 pixel < level-15 pixel).
 *
 * The fix: after the lightmap is computed, overwrite the whole 16×16 lightmap
 * texture with solid white. Then EVERY light coordinate — including (0,0) on those
 * covered faces — samples white, so they render exactly as bright as everything
 * else. Both vanilla and Sodium sample this same texture, so it works for both.
 * (Iris shaders compute their own lighting and ignore it.)
 *
 * Cost: one 16×16 GPU clear per frame while FULLBRIGHT is active — negligible.
 * Zero cost when off.
 */
@Mixin(Lightmap.class)
public class LightmapClearMixin {

    @Shadow @Final private GpuTexture texture;

    @Inject(method = "render(Lnet/minecraft/client/renderer/state/LightmapRenderState;)V", at = @At("TAIL"))
    private void bur$whiteLightmap(LightmapRenderState renderState, CallbackInfo ci) {
        if (HideState.isLightActive() && HideState.lightMode() == ModConfig.LightMode.FULLBRIGHT) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.texture, -1); // -1 = white
        }
    }
}

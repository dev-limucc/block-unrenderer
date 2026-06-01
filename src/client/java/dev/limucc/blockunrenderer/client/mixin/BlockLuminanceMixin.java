package dev.limucc.blockunrenderer.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * FULLBRIGHT via the Meteor-style "Luminance" technique.
 *
 * Every block reports a light emission of 15 while FULLBRIGHT is active. The renderer floors a
 * face's block-light with the block's own getLightEmission() (vanilla getLightColor does
 * Math.max(storedBlockLight, state.getLightEmission()); Sodium does the same), so EVERY visible
 * face — including covered faces that had stored light 0 — renders at full block light.
 *
 * getLightEmission() is called per-block during meshing/lighting, so we use the zero-allocation
 * {@link ModifyReturnValue} (no CallbackInfo per call). The map-sampling guard inside
 * isLightActive()/the mixin keeps fake luminance out of Xaero's map sampling.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockLuminanceMixin {

    @ModifyReturnValue(method = "getLightEmission", at = @At("RETURN"))
    private int bur$fullbright(int original) {
        if (HideState.isMapSampling()) return original;
        return HideState.isLightActive() ? 15 : original;
    }
}

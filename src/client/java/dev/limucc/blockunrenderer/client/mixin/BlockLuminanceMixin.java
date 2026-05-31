package dev.limucc.blockunrenderer.client.mixin;

import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FULLBRIGHT via the Meteor-style "Luminance" technique.
 *
 * Every block reports a light emission of 15 while FULLBRIGHT is active. The
 * renderer floors a face's block-light with the block's own getLightEmission()
 * (vanilla getLightColor does Math.max(storedBlockLight, state.getLightEmission());
 * Sodium does the same in its light pipeline). So EVERY visible face — including
 * the faces of blocks that were covered and had stored light 0 — renders at full
 * block light.
 *
 * No relight, no lightmap override, works for vanilla AND Sodium. Read live during
 * meshing, so it's effective immediately on the chunk re-mesh that toggling causes.
 * Zero cost when off (one boolean check).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockLuminanceMixin {

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void bur$fullbright(CallbackInfoReturnable<Integer> cir) {
        if (HideState.isLightActive() && HideState.lightMode() == ModConfig.LightMode.FULLBRIGHT) {
            cir.setReturnValue(15);
        }
    }
}

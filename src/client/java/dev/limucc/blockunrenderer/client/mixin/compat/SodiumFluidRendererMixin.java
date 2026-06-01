package dev.limucc.blockunrenderer.client.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides LIQUIDS under Sodium. Sodium replaces the vanilla fluid renderer with its own
 * {@code DefaultFluidRenderer}, so {@link dev.limucc.blockunrenderer.client.mixin.FluidRendererMixin}
 * (which targets vanilla {@code FluidRenderer}) never runs when Sodium is installed — that's why
 * water/lava didn't vanish.
 *
 * Soft-mixin ({@link Pseudo} + {@code targets}, in a non-required config) so it's harmless when
 * Sodium is absent or on a version with a different signature. We capture only the {@link FluidState}
 * argument via MixinExtras {@code @Local} so we never reference Sodium-internal types.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public class SodiumFluidRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void bur$hideSodiumFluid(CallbackInfo ci, @Local(argsOnly = true) FluidState fluidState) {
        if (HideState.shouldHideFluid(fluidState.getType())) {
            ci.cancel();
        }
    }
}

package dev.limucc.blockunrenderer.client.mixin;

import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides LIQUIDS (water, lava, modded fluids) — including waterlogged blocks and flowing
 * fluid, which the block render-shape mixin can't catch because fluids are meshed separately.
 *
 * tesselate() is the single per-fluid-cell gate the chunk mesher calls; cancelling it at HEAD
 * emits no fluid quads. Matched by the fluid's type so "minecraft:water" hides every water cell.
 * Zero cost when off (one boolean check guarded by HideState.active).
 */
@Mixin(FluidRenderer.class)
public class FluidRendererMixin {

    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void bur$hideFluid(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output,
                               BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        if (HideState.shouldHideFluid(fluidState.getType())) {
            ci.cancel();
        }
    }
}

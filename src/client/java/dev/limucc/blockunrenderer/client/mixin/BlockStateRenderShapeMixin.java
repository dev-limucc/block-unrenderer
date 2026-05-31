package dev.limucc.blockunrenderer.client.mixin;

import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides REGULAR blocks by making their render shape INVISIBLE.
 *
 * Both vanilla AND Sodium build chunk geometry by calling
 * BlockState.getRenderShape() and skipping anything that isn't RenderShape.MODEL
 * (see SectionCompiler line 99). By returning INVISIBLE here, the block is simply
 * never added to the chunk mesh — zero per-frame cost, fully Sodium-compatible.
 *
 * The block's collision, lighting, and logic are untouched; only its visual mesh
 * is removed. Toggling triggers a one-time chunk re-mesh (see HideState).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateRenderShapeMixin {

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void blockUnrenderer$hideRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        // `active` boolean is checked first inside shouldHide → negligible overhead when off
        BlockState state = (BlockState) (Object) this;
        if (HideState.shouldHide(state.getBlock())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}

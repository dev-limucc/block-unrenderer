package dev.limucc.blockunrenderer.client.mixin;

import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides REGULAR blocks AND keeps their neighbours rendered.
 *
 * Two parts:
 *
 * 1. getRenderShape() → INVISIBLE
 *    Both vanilla and Sodium skip anything that isn't RenderShape.MODEL when
 *    building chunk geometry, so the block itself is never meshed.
 *
 * 2. canOcclude()/isSolidRender() → false
 *    Face culling (Block.shouldRenderFace) renders a neighbour's face UNLESS the
 *    adjacent block canOcclude(). If we leave a hidden block occluding, the block
 *    underneath/around it loses the touching face → you see a hole into the void.
 *    Reporting the hidden block as non-occluding makes neighbours draw those faces,
 *    so e.g. hiding grass reveals the dirt's top face instead of a floorless hole.
 *    Hooking both methods covers vanilla (canOcclude) and Sodium (isSolidRender).
 *
 * All gated on HideState.shouldHide(), whose `active` check is near-zero cost when off.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateRenderShapeMixin {

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void blockUnrenderer$hideRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        BlockState state = (BlockState) (Object) this;
        if (HideState.shouldHide(state.getBlock())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void blockUnrenderer$noOcclude(CallbackInfoReturnable<Boolean> cir) {
        BlockState state = (BlockState) (Object) this;
        if (HideState.shouldHide(state.getBlock())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void blockUnrenderer$notSolidRender(CallbackInfoReturnable<Boolean> cir) {
        BlockState state = (BlockState) (Object) this;
        if (HideState.shouldHide(state.getBlock())) {
            cir.setReturnValue(false);
        }
    }

    /**
     * The shape-based face-culling path uses the CACHED occlusion shape (built at
     * state construction when canOcclude was still true). Returning an empty shape
     * here makes that path treat the hidden block as fully transparent too — the
     * final piece needed so neighbours always render their exposed faces.
     */
    @Inject(method = "getOcclusionShape()Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void blockUnrenderer$emptyOcclusionShape(CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = (BlockState) (Object) this;
        if (HideState.shouldHide(state.getBlock())) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}

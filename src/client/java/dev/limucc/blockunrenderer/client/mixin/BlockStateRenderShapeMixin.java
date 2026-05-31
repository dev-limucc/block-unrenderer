package dev.limucc.blockunrenderer.client.mixin;

import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.core.Direction;
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
 * Hides regular blocks, optionally keeps neighbours rendered, and optionally lets
 * light through. Every check tests HideState's `active` flag first → no cost when off.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateRenderShapeMixin {

    // Make the block itself invisible (skipped during chunk meshing — vanilla + Sodium).
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void bur$hideRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        if (HideState.shouldHide(((BlockState) (Object) this).getBlock())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }

    // ── Keep neighbours rendered (gated by "Show blocks underneath") ──────────

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void bur$noOcclude(CallbackInfoReturnable<Boolean> cir) {
        if (HideState.shouldShowUnder(((BlockState) (Object) this).getBlock())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void bur$notSolidRender(CallbackInfoReturnable<Boolean> cir) {
        if (HideState.shouldShowUnder(((BlockState) (Object) this).getBlock())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getFaceOcclusionShape", at = @At("HEAD"), cancellable = true)
    private void bur$emptyFaceOcclusion(Direction direction, CallbackInfoReturnable<VoxelShape> cir) {
        if (HideState.shouldShowUnder(((BlockState) (Object) this).getBlock())) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    // ── Let skylight through (gated by "Fix lighting") ────────────────────────
    // Data-level so it's consistent for vanilla, Sodium, and shader light reads.
    // (The brightness boost in HideState provides the immediate visibility.)

    @Inject(method = "propagatesSkylightDown", at = @At("HEAD"), cancellable = true)
    private void bur$skylightThrough(CallbackInfoReturnable<Boolean> cir) {
        if (HideState.shouldPassLight(((BlockState) (Object) this).getBlock())) {
            cir.setReturnValue(true);
        }
    }
}

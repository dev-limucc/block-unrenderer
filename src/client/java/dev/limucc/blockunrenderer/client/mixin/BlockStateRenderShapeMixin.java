package dev.limucc.blockunrenderer.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides regular blocks, optionally keeps neighbours rendered, and optionally lets light through.
 *
 * These methods are called per-block, millions of times during chunk meshing & lighting. We use
 * MixinExtras {@link ModifyReturnValue} (NOT {@code @Inject(cancellable=true)}) so NO CallbackInfo
 * object is allocated on any call — when the mod is off this is a single volatile read, the JIT
 * inlines it, and there is zero GC pressure. That allocation churn was the main cause of the
 * world-load lag on low-end machines.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateRenderShapeMixin {

    // Make the block invisible (skipped during chunk meshing — vanilla + Sodium).
    @ModifyReturnValue(method = "getRenderShape", at = @At("RETURN"))
    private RenderShape bur$hideRenderShape(RenderShape original) {
        return HideState.shouldHide(((BlockState) (Object) this).getBlock()) ? RenderShape.INVISIBLE : original;
    }

    // ── Keep neighbours rendered (gated by "Show blocks underneath") ──────────

    @ModifyReturnValue(method = "canOcclude", at = @At("RETURN"))
    private boolean bur$noOcclude(boolean original) {
        return original && !HideState.shouldShowUnder(((BlockState) (Object) this).getBlock());
    }

    @ModifyReturnValue(method = "isSolidRender", at = @At("RETURN"))
    private boolean bur$notSolidRender(boolean original) {
        return original && !HideState.shouldShowUnder(((BlockState) (Object) this).getBlock());
    }

    @ModifyReturnValue(method = "getFaceOcclusionShape", at = @At("RETURN"))
    private VoxelShape bur$emptyFaceOcclusion(VoxelShape original) {
        return HideState.shouldShowUnder(((BlockState) (Object) this).getBlock()) ? Shapes.empty() : original;
    }

    // ── Let skylight through (gated by FULLBRIGHT) ────────────────────────────

    @ModifyReturnValue(method = "propagatesSkylightDown", at = @At("RETURN"))
    private boolean bur$skylightThrough(boolean original) {
        return original || HideState.shouldPassLight(((BlockState) (Object) this).getBlock());
    }
}

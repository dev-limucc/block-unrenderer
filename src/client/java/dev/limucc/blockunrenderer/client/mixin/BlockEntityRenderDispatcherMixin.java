package dev.limucc.blockunrenderer.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides BLOCK ENTITIES (chests, furnaces, signs, beds, etc.).
 *
 * Block entities like chests have an INVISIBLE block model — their visuals come entirely from a
 * per-frame BlockEntityRenderer, so the render-shape mixin does nothing for them. Instead we drop
 * their render state here. tryExtractRenderState() is the single per-block-entity gate the
 * dispatcher uses each frame; returning null means "nothing to render". Independent of Sodium,
 * instant (no chunk rebuild).
 *
 * Uses MixinExtras {@link ModifyReturnValue} (the extra original-call args are appended after the
 * return value) so no CallbackInfo is allocated per call.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @ModifyReturnValue(method = "tryExtractRenderState", at = @At("RETURN"))
    private BlockEntityRenderState bur$hideBlockEntity(BlockEntityRenderState original,
                                                       BlockEntity blockEntity, float partialTicks,
                                                       ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        return HideState.shouldHide(blockEntity.getBlockState().getBlock()) ? null : original;
    }
}

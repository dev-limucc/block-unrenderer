package dev.limucc.blockunrenderer.client.mixin;

import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides BLOCK ENTITIES (chests, furnaces, signs, beds, etc.).
 *
 * Block entities like chests have an INVISIBLE block model — their visuals come
 * entirely from a per-frame BlockEntityRenderer, so the render-shape mixin does
 * nothing for them. Instead we cancel their render-state extraction here.
 *
 * tryExtractRenderState() is the single per-block-entity gate the dispatcher uses
 * each frame; returning null means "nothing to render". This is independent of
 * Sodium (Sodium doesn't replace block-entity rendering), and it's instant — no
 * chunk rebuild needed. Cost is one O(1) set lookup per visible block entity.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void blockUnrenderer$hideBlockEntity(BlockEntity blockEntity, float partialTicks,
                                                 ModelFeatureRenderer.CrumblingOverlay breakProgress,
                                                 CallbackInfoReturnable<BlockEntityRenderState> cir) {
        if (HideState.shouldHide(blockEntity.getBlockState().getBlock())) {
            cir.setReturnValue(null);
        }
    }
}

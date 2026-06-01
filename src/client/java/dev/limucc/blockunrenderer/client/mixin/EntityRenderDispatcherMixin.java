package dev.limucc.blockunrenderer.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides ALL entities (mobs, items, projectiles, etc.) when "Hide entities" is on.
 *
 * shouldRender() is the per-entity visibility gate asked each frame; we force it false via the
 * zero-allocation {@link ModifyReturnValue}. When off, a single volatile read — no CallbackInfo.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private boolean bur$hideEntities(boolean original) {
        return original && !HideState.shouldHideEntities();
    }
}

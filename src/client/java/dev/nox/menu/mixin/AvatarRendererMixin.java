package dev.nox.menu.mixin;

import dev.nox.menu.NoxMenuClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks the render state of the local player so cosmetics / emotes only apply to you. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"), require = 0)
	private void nox$markLocal(AvatarlikeEntity entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		state.setData(NoxMenuClient.LOCAL, (Object) entity == (Object) Minecraft.getInstance().player);
	}
}

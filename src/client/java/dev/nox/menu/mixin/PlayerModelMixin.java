package dev.nox.menu.mixin;

import dev.nox.menu.Emotes;
import dev.nox.menu.NoxMenuClient;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After vanilla poses the model, overlay the active emote (local player only). */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("RETURN"), require = 0)
	private void nox$emote(AvatarRenderState state, CallbackInfo ci) {
		Boolean local = state.getData(NoxMenuClient.LOCAL);
		if (local == null || !local) return;
		Emotes.apply(((PlayerModel) (Object) this).root());
	}
}

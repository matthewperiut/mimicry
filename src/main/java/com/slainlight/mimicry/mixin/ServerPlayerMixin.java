package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.CastlePiece;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void mimicry$restoreKeep(CallbackInfo ci) {
		CastlePiece.tickRestore((ServerPlayer) (Object) this);
	}
}

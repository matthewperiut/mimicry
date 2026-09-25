package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.Mimicry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if <26.1 {
/*import net.minecraft.world.item.context.BlockPlaceContext;
*///?}

// chests placed next to a Mimic Chest don't pair up with it
@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {
	//? if >=26.1 {
	@Inject(method = "candidatePartnerFacing", at = @At("HEAD"), cancellable = true)
	private void mimicry$skipMimicChests(Level level, BlockPos pos, Direction neighbourDirection, CallbackInfoReturnable<Direction> cir) {
		if (Mimicry.isMimicChest(level, pos.relative(neighbourDirection))) {
			cir.setReturnValue(null);
		}
	}
	//?} else {
	/*@Inject(method = "candidatePartnerFacing", at = @At("HEAD"), cancellable = true)
	private void mimicry$skipMimicChests(BlockPlaceContext context, Direction neighbourDirection, CallbackInfoReturnable<Direction> cir) {
		Level level = context.getLevel();
		if (Mimicry.isMimicChest(level, context.getClickedPos().relative(neighbourDirection))) {
			cir.setReturnValue(null);
		}
	}
	*///?}
}

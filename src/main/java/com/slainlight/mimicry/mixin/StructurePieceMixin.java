package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.PrimedChests;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin implements PrimedChests.PieceHook {
	// not synchronized: two chunks of one piece generating in parallel can each add a companion, which is harmless
	@Unique
	private boolean mimicry$companionPlaced;

	@Inject(
		method = "createChest(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;)Z",
		at = @At("RETURN")
	)
	private void mimicry$addCompanion(
		ServerLevelAccessor level, BoundingBox chunkBB, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable, @Nullable BlockState state,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (cir.getReturnValueZ()) {
			this.mimicry$onLootChest(level, chunkBB, random, pos, lootTable);
		}
	}

	@Override
	public void mimicry$onLootChest(ServerLevelAccessor level, BoundingBox chunkBB, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable) {
		if (!this.mimicry$companionPlaced) {
			BoundingBox pieceBB = ((StructurePiece) (Object) this).getBoundingBox();
			this.mimicry$companionPlaced = PrimedChests.placeCompanion(level, random, pos, lootTable, PrimedChests.insetBy1(chunkBB).and(pieceBB::isInside));
		}
	}
}

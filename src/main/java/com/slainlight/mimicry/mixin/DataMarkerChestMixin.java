package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.PrimedChests;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// these set chest loot from data markers rather than through StructurePiece.createChest
@Mixin({ShipwreckPieces.ShipwreckPiece.class, EndCityPieces.EndCityPiece.class})
public abstract class DataMarkerChestMixin {
	@WrapOperation(
		method = "handleDataMarker",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/RandomizableContainer;setBlockEntityLootTable(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceKey;)V"
		)
	)
	private void mimicry$addCompanion(
		BlockGetter level, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable, Operation<Void> original,
		@Local(argsOnly = true) ServerLevelAccessor levelAccessor, @Local(argsOnly = true) BoundingBox chunkBB
	) {
		original.call(level, random, pos, lootTable);
		((PrimedChests.PieceHook) this).mimicry$onLootChest(levelAccessor, chunkBB, random, pos, lootTable);
	}
}

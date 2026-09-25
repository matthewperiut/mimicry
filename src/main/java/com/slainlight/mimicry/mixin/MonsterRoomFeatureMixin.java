package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.PrimedChests;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
//? if <26.3 {
/*import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
*///?}
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MonsterRoomFeature.class)
public abstract class MonsterRoomFeatureMixin {
	@Inject(method = "place", at = @At("RETURN"))
	//? if >=26.3 {
	private void mimicry$addCompanion(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
	//?} else {
	/*private void mimicry$addCompanion(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
	*///?}
		if (cir.getReturnValueZ()) {
			// room floor only (interior is within 3 of the origin); the dungeon itself already writes across chunk borders
			PrimedChests.placeCompanion(level, random, origin, BuiltInLootTables.SIMPLE_DUNGEON,
				pos -> pos.getY() == origin.getY() && Math.abs(pos.getX() - origin.getX()) <= 3 && Math.abs(pos.getZ() - origin.getZ()) <= 3);
		}
	}
}

package com.slainlight.mimicry.mixin;

//? if >=26.3 {
import com.slainlight.mimicry.Earthworks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// every placed feature goes through here, nested ones included
@Mixin(FeaturePlacer.class)
public abstract class FeaturePlacerMixin {
	@WrapOperation(
		method = "place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/feature/Feature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
		)
	)
	private boolean mimicry$keepGladesClear(Feature feature, WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
		Operation<Boolean> original) {
		return !Earthworks.keepsClear(feature, origin) && original.call(feature, level, generator, random, origin);
	}
}
//?}

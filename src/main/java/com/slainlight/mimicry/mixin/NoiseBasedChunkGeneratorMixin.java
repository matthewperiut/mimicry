package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.Earthworks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// adds the Wayside Forge's earthworks to the terrain noise alongside vanilla's structure beards
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
	@WrapOperation(
		method = "createNoiseChunk",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/Beardifier;forStructuresInChunk(Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/world/level/levelgen/Beardifier;"
		)
	)
	private Beardifier mimicry$earthworks(StructureManager structures, ChunkPos pos, Operation<Beardifier> original,
		@Local(argsOnly = true) ChunkAccess chunk, @Local(argsOnly = true) RandomState randomState) {
		return Earthworks.around(original.call(structures, pos), structures, chunk, (NoiseBasedChunkGenerator) (Object) this, randomState);
	}
}

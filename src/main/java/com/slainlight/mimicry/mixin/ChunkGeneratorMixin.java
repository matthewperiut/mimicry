package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.Earthworks;
import com.slainlight.mimicry.WaysideForge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	// sets the glades FeaturePlacerMixin keeps clear while this chunk's features are placed
	@Inject(method = "applyBiomeDecoration", at = @At("HEAD"))
	private void mimicry$markGlades(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
		Earthworks.beginDecorating(structures, chunk.getPos());
	}

	@Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
	private void mimicry$unmarkGlades(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
		Earthworks.endDecorating();
	}

	// the forge's structure start is in its keep's chunk, so point /locate and explorer maps at the forge piece instead
	@WrapOperation(
		method = "getStructureGeneratingAt",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/placement/StructurePlacement;getLocatePos(Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/core/BlockPos;"
		)
	)
	private static BlockPos mimicry$locateForge(StructurePlacement placement, ChunkPos pos, Operation<BlockPos> original, @Local Holder<Structure> structure,
		@Local(argsOnly = true) LevelReader level, @Local(argsOnly = true) StructureManager structures) {
		if (structure.value() instanceof WaysideForge) {
			StructureStart start = structures.getStartForStructure(structure.value(), level.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_STARTS));
			if (start != null && start.isValid()) {
				return start.getPieces().stream().filter(piece -> piece instanceof WaysideForge.Piece).findFirst()
					.map(piece -> piece.getBoundingBox().getCenter().atY(piece.getBoundingBox().minY()))
					.orElseGet(() -> original.call(placement, pos));
			}
		}
		return original.call(placement, pos);
	}
}

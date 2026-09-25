package com.slainlight.mimicry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;

public class CastlePiece extends TemplateStructurePiece {
	static final Identifier TEMPLATE = Mimicry.id("castle");
	// template is 43 x 34 x 43; the floor is 3 blocks up and the centre column is at (21, 21)
	static final int G = 3;
	static final int C = 21;
	private static final BlockPos PIVOT = new BlockPos(C, 0, C);

	CastlePiece(StructureTemplateManager templates, BlockPos center, int floorY, Direction approach) {
		super(Hollowmere.KEEP_CASTLE, 0, templates, TEMPLATE, TEMPLATE.toString(), settings(rotationFor(approach)),
			new BlockPos(center.getX() - C, floorY - G, center.getZ() - C));
	}

	public CastlePiece(StructureTemplateManager templates, CompoundTag tag) {
		super(Hollowmere.KEEP_CASTLE, tag, templates, id -> settings(Rotation.valueOf(tag.getStringOr("Rot", "NONE"))));
	}

	// the template's gate is on its north side, so rotate its south axis (gate to donjon) onto the approach
	static Rotation rotationFor(Direction approach) {
		for (Rotation rotation : Rotation.values()) {
			if (rotation.rotate(Direction.SOUTH) == approach) {
				return rotation;
			}
		}
		return Rotation.NONE;
	}

	private static StructurePlaceSettings settings(Rotation rotation) {
		return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(PIVOT).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", this.placeSettings.getRotation().name());
	}

	@Override
	protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
		if (!chunkBB.isInside(pos)) {
			return;
		}
		Direction gate = this.placeSettings.getRotation().rotate(Direction.NORTH);
		switch (marker) {
			case "knight" -> {
				MossKnightEntity knight = Hollowmere.MOSS_KNIGHT.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (knight != null) {
					knight.snapTo(Vec3.atBottomCenterOf(pos), gate.toYRot(), 0.0F);
					knight.setYBodyRot(gate.toYRot());
					knight.setYHeadRot(gate.toYRot());
					knight.guard(pos);
					knight.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
					level.addFreshEntityWithPassengers(knight);
				}
			}
			case "mimic" -> {
				MimicEntity mimic = Mimicry.MIMIC.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (mimic != null) {
					float yaw = gate.getClockWise().toYRot();
					mimic.snapTo(Vec3.atBottomCenterOf(pos), yaw, 0.0F);
					mimic.setYBodyRot(yaw);
					mimic.setYHeadRot(yaw);
					mimic.setDormant(true);
					mimic.setPersistenceRequired();
					level.addFreshEntity(mimic);
				}
			}
			default -> {
			}
		}
	}
}

package com.slainlight.mimicry;

import com.slainlight.mimicry.platform.Platform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

public class CastlePiece extends TemplateStructurePiece {
	static final Identifier TEMPLATE = Mimicry.id("castle");
	// template is 43 x 34 x 43; the floor is 3 blocks up and the centre column is at (21, 21)
	static final int G = 3;
	static final int C = 21;
	private static final BlockPos PIVOT = new BlockPos(C, 0, C);
	// King's Coffer position in the template
	private static final BlockPos THRONE = new BlockPos(C, G + 2, 26);
	private static final TagKey<Structure> KEEPS = TagKey.create(Registries.STRUCTURE, Mimicry.id("on_keep_maps"));

	CastlePiece(StructureTemplateManager templates, BlockPos center, int floorY, Direction approach) {
		super(Hollowmere.KEEP_CASTLE, 0, templates, TEMPLATE, TEMPLATE.toString(), settings(rotationFor(approach)),
			new BlockPos(center.getX() - C, floorY - G, center.getZ() - C));
	}

	public CastlePiece(StructureTemplateManager templates, CompoundTag tag) {
		super(Hollowmere.KEEP_CASTLE, tag, templates, id -> settings(Rotation.valueOf(tag./*? if >=1.21.5 {*/getStringOr("Rot", "NONE")/*?} else {*//*getString("Rot")*//*?}*/)));
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
		switch (marker) {
			case "knight" -> this.spawnKnight(level, pos);
			case "mimic" -> {
				MimicEntity mimic = Mimicry.MIMIC.create(level.getLevel()/*? if >=1.21.2 {*/, EntitySpawnReason.STRUCTURE/*?}*/);
				if (mimic != null) {
					float yaw = this.gate().getClockWise().toYRot();
					mimic./*? if >=26.1 {*/snapTo/*?} else {*//*moveTo*//*?}*/(pos, yaw, 0.0F);
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

	private Direction gate() {
		return this.placeSettings.getRotation().rotate(Direction.NORTH);
	}

	private void spawnKnight(ServerLevelAccessor level, BlockPos pos) {
		MossKnightEntity knight = Hollowmere.MOSS_KNIGHT.create(level.getLevel()/*? if >=1.21.2 {*/, EntitySpawnReason.STRUCTURE/*?}*/);
		if (knight != null) {
			float yaw = this.gate().toYRot();
			knight./*? if >=26.1 {*/snapTo/*?} else {*//*moveTo*//*?}*/(pos, yaw, 0.0F);
			knight.setYBodyRot(yaw);
			knight.setYHeadRot(yaw);
			knight.guard(pos, this.id());
			knight.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null/*? if <1.20.5 {*//*, null*//*?}*/);
			level.addFreshEntityWithPassengers(knight);
		}
	}

	private long id() {
		return this.templatePosition.asLong();
	}

	// set when Bram's quest is accepted; the next keep the player enters is restored
	public static void requestRestore(ServerPlayer player) {
		Platform.keepData(player).putBoolean("Restore", true);
		Platform.markKeepDataDirty(player);
	}

	public static void tickRestore(ServerPlayer player) {
		if (player.tickCount % 20 != 0 || !Platform.keepData(player).contains("Restore")) {
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		for (StructurePiece piece : level.structureManager().getStructureWithPieceAt(player.blockPosition(), KEEPS).getPieces()) {
			if (piece instanceof CastlePiece castle && castle.restore(level)) {
				Platform.keepData(player).remove("Restore");
				Platform.markKeepDataDirty(player);
			}
		}
	}

	// respawns knights at empty posts and the King's Coffer on its throne. Waits until entities around the castle are
	// loaded so unloaded knights aren't duplicated
	private boolean restore(ServerLevel level) {
		BoundingBox area = this.boundingBox.inflatedBy(16);
		for (int x = SectionPos.blockToSectionCoord(area.minX()); x <= SectionPos.blockToSectionCoord(area.maxX()); x++) {
			for (int z = SectionPos.blockToSectionCoord(area.minZ()); z <= SectionPos.blockToSectionCoord(area.maxZ()); z++) {
				if (!level.areEntitiesLoaded(ChunkPos./*? if >=26.1 {*/pack/*?} else {*//*asLong*//*?}*/(x, z))) {
					return false;
				}
			}
		}
		AABB box = AABB.of(area);
		// placeSettings can still hold the bounds of the last chunk the piece was placed in
		StructurePlaceSettings settings = settings(this.placeSettings.getRotation());
		List<BlockPos> posts = new ArrayList<>();
		for (StructureTemplate.StructureBlockInfo marker : this.template.filterBlocks(this.templatePosition, settings, Blocks.STRUCTURE_BLOCK)) {
			if (marker.nbt() != null && "knight".equals(marker.nbt()./*? if >=1.21.5 {*/getStringOr("metadata", "")/*?} else {*//*getString("metadata")*//*?}*/)) {
				posts.add(marker.pos());
			}
		}
		List<MossKnightEntity> knights = level.getEntitiesOfClass(MossKnightEntity.class, box, LivingEntity::isAlive);
		posts.removeIf(post -> knights.stream().anyMatch(knight -> knight.guards(this.id(), post)));
		// knights from before posts were saved take the nearest empty one
		for (MossKnightEntity knight : knights) {
			if (!knight.hasPost() && knight.isPersistenceRequired()) {
				posts.stream().min(Comparator.comparingDouble(post -> post.distSqr(knight.blockPosition()))).ifPresent(post -> {
					knight.guard(post, this.id());
					posts.remove(post);
				});
			}
		}
		posts.forEach(post -> this.spawnKnight(level, post));

		BlockPos throne = StructureTemplate.calculateRelativePosition(settings, THRONE).offset(this.templatePosition);
		if (!level.getBlockState(throne).is(Blocks.CHEST) && level.getEntitiesOfClass(MimicEntity.class, box, mimic -> mimic.isKing() && mimic.isAlive()).isEmpty()) {
			// also rebuilds the throne the King broke out of
			settings.setBoundingBox(new BoundingBox(throne.getX() - 1, throne.getY(), throne.getZ() - 1, throne.getX() + 1, throne.getY() + 2, throne.getZ() + 1))
				.setIgnoreEntities(true);
			this.template.placeInWorld(level, this.templatePosition, this.templatePosition, settings, level.getRandom(), Block.UPDATE_CLIENTS);
		}
		return true;
	}
}

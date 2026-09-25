package com.slainlight.mimicry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;

// a loot table gets a companion chest only if mimicry:primed/<namespace>/<path> exists, so datapacks can opt structures in
public final class PrimedChests {
	private static final int SEARCH_RADIUS = 4;

	public interface PieceHook {
		void mimicry$onLootChest(ServerLevelAccessor level, BoundingBox chunkBB, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable);
	}

	private PrimedChests() {
	}

	public static boolean isPrimed(ResourceKey<LootTable> lootTable) {
		return lootTable.identifier().getNamespace().equals(Mimicry.MOD_ID) && lootTable.identifier().getPath().startsWith("primed/");
	}

	// keeps all four neighbours in the box too, since blocks in ungenerated chunks would fool the wall check
	public static Predicate<BlockPos> insetBy1(BoundingBox box) {
		return pos -> pos.getX() > box.minX() && pos.getX() < box.maxX() && pos.getZ() > box.minZ() && pos.getZ() < box.maxZ() && box.isInside(pos);
	}

	public static boolean placeCompanion(ServerLevelAccessor level, RandomSource random, BlockPos origin, ResourceKey<LootTable> lootTable, Predicate<BlockPos> allowed) {
		Identifier source = lootTable.identifier();
		if (source.getNamespace().equals(Mimicry.MOD_ID)) {
			return false;
		}
		ServerLevel serverLevel = level.getLevel();
		ResourceKey<LootTable> primed = ResourceKey.create(Registries.LOOT_TABLE, Mimicry.id("primed/" + source.getNamespace() + "/" + source.getPath()));
		if (serverLevel.getGameRules().get(Mimicry.PRIMED_MIMIC_CHANCE) == 0
			|| serverLevel.getServer().reloadableRegistries().getLootTable(primed) == LootTable.EMPTY) {
			return false;
		}

		List<BlockPos> spots = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -1, -SEARCH_RADIUS), origin.offset(SEARCH_RADIUS, 1, SEARCH_RADIUS))) {
			if (allowed.test(pos) && isGoodSpot(level, pos)) {
				spots.add(pos.immutable());
			}
		}
		if (spots.isEmpty()) {
			return false;
		}

		BlockPos pos = spots.get(random.nextInt(spots.size()));
		BlockState chest = Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.FACING, facingAwayFromWall(level, pos))
			.setValue(ChestBlock.WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
		level.setBlock(pos, chest, Block.UPDATE_CLIENTS);
		RandomizableContainer.setBlockEntityLootTable(level, random, pos, primed);
		return true;
	}

	// unlike StructurePiece.reorient, also handles corners and nooks
	private static Direction facingAwayFromWall(LevelReader level, BlockPos pos) {
		for (Direction back : Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(pos.relative(back)).isSolidRender() && !level.getBlockState(pos.relative(back.getOpposite())).isSolidRender()) {
				return back.getOpposite();
			}
		}
		return Direction.NORTH;
	}

	private static boolean isGoodSpot(LevelReader level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.isAir() && !(state.is(Blocks.WATER) && state.getFluidState().isSource())) {
			return false;
		}
		BlockPos below = pos.below();
		BlockPos above = pos.above();
		if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || level.getBlockState(above).isRedstoneConductor(level, above)) {
			return false;
		}
		int walls = 0;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockState neighbor = level.getBlockState(pos.relative(direction));
			if (neighbor.is(Blocks.CHEST)) {
				return false;
			}
			if (neighbor.isSolidRender()) {
				walls++;
			}
		}
		boolean corridor = walls == 2 && level.getBlockState(pos.north()).isSolidRender() == level.getBlockState(pos.south()).isSolidRender();
		return walls >= 1 && walls <= 3 && !corridor;
	}
}

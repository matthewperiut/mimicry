package com.slainlight.mimicry;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

// only in dev: stamps the templates onto a flat world for hand editing, see tools/import_workshop.py
final class Workshop {
	static final int FORGE_X = 0;
	static final int FORGE_Z = 0;
	static final int CASTLE_X = 64;
	static final int CASTLE_Z = 8;

	private Workshop() {
	}

	static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mimicry")
			.then(Commands.literal("workshop").executes(context -> {
				build(context.getSource().getLevel());
				context.getSource().sendSuccess(() -> Component.literal("Workshop built: forge at " + FORGE_X + ", " + FORGE_Z
					+ ", castle around " + CASTLE_X + ", " + CASTLE_Z), true);
				return 1;
			})));
	}

	// positions must match BUILDS in tools/import_workshop.py
	static void build(ServerLevel level) {
		int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, FORGE_X, FORGE_Z) - 1;
		stamp(level, WaysideForge.TEMPLATE, new BlockPos(FORGE_X, ground + 1, FORGE_Z), 0);
		stamp(level, CastlePiece.TEMPLATE, new BlockPos(CASTLE_X - CastlePiece.C, ground - CastlePiece.G, CASTLE_Z - CastlePiece.C), CastlePiece.G);
	}

	private static void stamp(ServerLevel level, Identifier id, BlockPos origin, int below) {
		StructureTemplate template = level.getServer()./*? if >=26.3 {*/getStructureTemplateManager/*?} else {*//*getStructureManager*//*?}*/().getOrCreate(id);
		template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
		BlockPos corner = origin.offset(-1, below, -1);
		BlockState state = Blocks.STRUCTURE_BLOCK.defaultBlockState().setValue(StructureBlock.MODE, StructureMode.SAVE);
		level.setBlock(corner, state, Block.UPDATE_ALL);
		if (level.getBlockEntity(corner) instanceof StructureBlockEntity block) {
			block.setMode(StructureMode.SAVE);
			block.setStructureName(id);
			block.setStructurePos(new BlockPos(1, -below, 1));
			block.setStructureSize(new Vec3i(template.getSize().getX(), template.getSize().getY(), template.getSize().getZ()));
			block.setIgnoreEntities(false);
			block.setShowBoundingBox(true);
			block.setChanged();
			level.sendBlockUpdated(corner, state, state, Block.UPDATE_ALL);
		}
	}
}

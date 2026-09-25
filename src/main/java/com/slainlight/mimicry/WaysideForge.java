package com.slainlight.mimicry;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;

// uses SunkenKeep's placement grid and re-plans the keep for the same start chunk; sites are 80-104 blocks from the
// portal so the forge and its grounds stay within 8 chunks of the start chunk
public class WaysideForge extends Structure {
	public static final MapCodec<WaysideForge> CODEC = simpleCodec(WaysideForge::new);
	static final Identifier TEMPLATE = Mimicry.id("wayside_forge");
	// template is 23x19x21 with the gate at z = 0; rotates about its center
	private static final BlockPos PIVOT = new BlockPos(11, 0, 10);
	private static final int MAX_FILL = 12;
	private static final int MAX_CUT = 14;

	public WaysideForge(StructureSettings settings) {
		super(settings);
	}

	@Override
	public StructureType<?> type() {
		return Hollowmere.WAYSIDE_FORGE;
	}

	record Site(int x, int z, int floor, double score) {
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		Optional<SunkenKeep.Plan> keep = SunkenKeep.plan(context);
		if (keep.isEmpty() || !keepBelongsHere(context, keep.get().portal())) {
			return Optional.empty();
		}
		BlockPos portal = keep.get().portal();
		Site best = site(context, keep.get());
		if (best == null) {
			return Optional.empty();
		}
		int dx = portal.getX() - best.x();
		int dz = portal.getZ() - best.z();
		Direction toPortal = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
		Rotation rotation = Arrays.stream(Rotation.values()).filter(r -> r.rotate(Direction.NORTH) == toPortal).findFirst().orElse(Rotation.NONE);
		BlockPos center = new BlockPos(best.x(), best.floor(), best.z());
		BlockPos origin = center.subtract(PIVOT);
		return Optional.of(new GenerationStub(center, builder -> {
			Piece forge = new Piece(context.structureTemplateManager(), origin, rotation);
			builder.addPiece(new Grounds(forge.getBoundingBox()));
			builder.addPiece(forge);
		}));
	}

	// SunkenKeep also calls this and skips generation on null, so it must stay deterministic for a given plan
	static Site site(GenerationContext context, SunkenKeep.Plan keep) {
		BlockPos portal = keep.portal();
		Direction back = keep.dir().getOpposite();
		double behind = Math.atan2(back.getStepZ(), back.getStepX());
		Site best = null;
		for (int ring : new int[] {80, 92, 104}) {
			for (int k = -3; k <= 3; k++) {
				double angle = behind + k * Math.toRadians(22);
				int x = portal.getX() + (int) Math.round(Math.cos(angle) * ring);
				int z = portal.getZ() + (int) Math.round(Math.sin(angle) * ring);
				Site site = survey(context, x, z, Math.abs(k) + (ring - 80) / 12.0);
				if (site != null && (best == null || site.score() < best.score())) {
					best = site;
				}
			}
		}
		return best;
	}

	private static boolean keepBelongsHere(GenerationContext context, BlockPos portal) {
		Holder<Biome> biome = context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(portal.getX()), QuartPos.fromBlock(portal.getY()),
			QuartPos.fromBlock(portal.getZ()));
		return context.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOptional(Mimicry.id("sunken_keep"))
			.map(keep -> keep.biomes().contains(biome)).orElse(false);
	}

	private static Site survey(GenerationContext context, int x, int z, double bias) {
		int[] ground = new int[25];
		int waterTop = Integer.MIN_VALUE;
		int i = 0;
		for (int dx = -12; dx <= 12; dx += 6) {
			for (int dz = -12; dz <= 12; dz += 6) {
				int solid = SunkenKeep.height(context, x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG) - 1;
				int top = SunkenKeep.height(context, x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG) - 1;
				ground[i++] = solid;
				if (top > solid) {
					waterTop = Math.max(waterTop, top);
				}
			}
		}
		Arrays.sort(ground);
		int floor = Math.max(ground[12], waterTop + 1);
		if (floor - ground[0] > MAX_FILL || ground[24] - floor > MAX_CUT) {
			return null;
		}
		Holder<Biome> biome = context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(floor), QuartPos.fromBlock(z));
		if (!context.validBiome().test(biome)) {
			return null;
		}
		double earthwork = 0;
		for (int g : ground) {
			earthwork += Math.abs(g - floor);
		}
		return new Site(x, z, floor, earthwork + (waterTop > Integer.MIN_VALUE ? 30 : 0) + bias * 6);
	}

	public static class Piece extends TemplateStructurePiece {
		Piece(StructureTemplateManager templates, BlockPos origin, Rotation rotation) {
			super(Hollowmere.FORGE, 0, templates, TEMPLATE, TEMPLATE.toString(), settings(rotation), origin);
		}

		public Piece(StructureTemplateManager templates, CompoundTag tag) {
			super(Hollowmere.FORGE, tag, templates, id -> settings(Rotation.valueOf(tag.getStringOr("Rot", "NONE"))));
		}

		private static StructurePlaceSettings settings(Rotation rotation) {
			return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(PIVOT).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			super.addAdditionalSaveData(context, tag);
			tag.putString("Rot", this.placeSettings.getRotation().name());
		}

		// the template's air would clear trees that grew into the box, so logs and leaves are restored after placing
		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB,
			ChunkPos chunkPos, BlockPos referencePos) {
			BoundingBox box = this.getBoundingBox();
			int floor = box.minY();
			java.util.Map<BlockPos, BlockState> trees = new java.util.HashMap<>();
			java.util.Map<BlockPos, BlockState> ground = new java.util.HashMap<>();
			for (int x = Math.max(box.minX(), chunkBB.minX()); x <= Math.min(box.maxX(), chunkBB.maxX()); x++) {
				for (int z = Math.max(box.minZ(), chunkBB.minZ()); z <= Math.min(box.maxZ(), chunkBB.maxZ()); z++) {
					ground.put(new BlockPos(x, floor, z), level.getBlockState(new BlockPos(x, floor, z)));
					for (int y = floor + 1; y <= box.maxY(); y++) {
						BlockPos pos = new BlockPos(x, y, z);
						BlockState state = level.getBlockState(pos);
						if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
							trees.put(pos, state);
						}
					}
				}
			}
			super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
			trees.forEach((pos, state) -> {
				if (level.getBlockState(pos).isAir()) {
					level.setBlock(pos, state, Block.UPDATE_CLIENTS);
				}
			});
			ground.forEach((pos, natural) -> {
				if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
					BlockState local = yardGround(natural, pos, level.getBlockState(pos.above()).isAir());
					if (local != null) {
						level.setBlock(pos, local, Block.UPDATE_CLIENTS);
					}
				}
			});
		}

		private static BlockState yardGround(BlockState natural, BlockPos pos, boolean bare) {
			if (natural.is(Blocks.PODZOL) || natural.is(Blocks.MYCELIUM) || natural.is(Blocks.MOSS_BLOCK) || natural.is(Blocks.PALE_MOSS_BLOCK)
				|| natural.is(Blocks.COARSE_DIRT) || natural.is(Blocks.ROOTED_DIRT) || natural.is(Blocks.MUD)) {
				return natural;
			}
			boolean rocky = natural.is(BlockTags.BASE_STONE_OVERWORLD) || natural.is(BlockTags.SAND) || natural.is(Blocks.GRAVEL)
				|| natural.is(BlockTags.TERRACOTTA) || natural.is(Blocks.SNOW_BLOCK) || natural.is(Blocks.CALCITE);
			float mix = (Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()) >>> 16 & 0xFFFF) / 65536.0F;
			return rocky && bare && mix < 0.45F ? natural : null;
		}

		@Override
		protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
			if (!marker.equals("blacksmith") || !chunkBB.isInside(pos)) {
				return;
			}
			BlacksmithEntity smith = Hollowmere.BLACKSMITH.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (smith == null) {
				return;
			}
			BlockPos anvil = this.find(Blocks.ANVIL).orElse(pos);
			BlockPos bed = Blocks.BED.asList().stream().flatMap(block -> this.template.filterBlocks(this.templatePosition, this.placeSettings, block).stream())
				.filter(info -> info.state().getValue(BedBlock.PART) == BedPart.HEAD).map(StructureTemplate.StructureBlockInfo::pos).findFirst().orElse(pos);
			smith.snapTo(Vec3.atBottomCenterOf(pos), this.placeSettings.getRotation().rotate(Direction.NORTH).toYRot(), 0.0F);
			smith.settle(anvil, bed);
			smith.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
			level.addFreshEntityWithPassengers(smith);
		}

		private Optional<BlockPos> find(Block block) {
			return this.template.filterBlocks(this.templatePosition, this.placeSettings, block).stream().map(StructureTemplate.StructureBlockInfo::pos).findFirst();
		}
	}

	// places no blocks: Earthworks blends terrain toward it during noise generation, and the inflated bounding box
	// lets every chunk within REACH find it
	public static class Grounds extends StructurePiece {
		static final int REACH = 18;
		private final BoundingBox footprint;

		Grounds(BoundingBox forge) {
			super(Hollowmere.FORGE_GROUNDS, 0, forge.inflatedBy(REACH, 0, REACH).encapsulate(
				new BoundingBox(forge.minX(), forge.minY() - MAX_FILL - 8, forge.minZ(), forge.maxX(), forge.maxY() + MAX_CUT, forge.maxZ())));
			this.footprint = forge;
		}

		public Grounds(CompoundTag tag) {
			super(Hollowmere.FORGE_GROUNDS, tag);
			int[] box = tag.getIntArray("Footprint").orElse(new int[6]);
			this.footprint = new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			BoundingBox f = this.footprint;
			tag.putIntArray("Footprint", new int[] {f.minX(), f.minY(), f.minZ(), f.maxX(), f.maxY(), f.maxZ()});
		}

		int floor() {
			return this.footprint.minY();
		}

		// 0 on the footprint, 1 where the natural terrain is left untouched
		double blend(int x, int z) {
			int dx = Math.max(0, Math.max(this.footprint.minX() - x, x - this.footprint.maxX()));
			int dz = Math.max(0, Math.max(this.footprint.minZ() - z, z - this.footprint.maxZ()));
			if (dx == 0 && dz == 0) {
				return 0.0;
			}
			double angle = Math.atan2(z - (this.footprint.minZ() + this.footprint.maxZ()) / 2.0, x - (this.footprint.minX() + this.footprint.maxX()) / 2.0);
			double phase = Math.floorMod(this.footprint.minX() * 31 + this.footprint.minZ() * 17, 628) / 100.0;
			double reach = REACH * (0.8 + 0.12 * Math.sin(3 * angle + phase) + 0.08 * Math.sin(7 * angle + 1.3));
			return Math.min(1.0, Math.sqrt(dx * dx + dz * dz) / reach);
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos referencePos) {
		}
	}
}

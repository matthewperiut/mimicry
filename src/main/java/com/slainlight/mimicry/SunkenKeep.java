package com.slainlight.mimicry;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

// pieces are added in order (cavern, tunnel, castle) and are pure functions of position and the stored seed,
// so each chunk can build its slice independently
public class SunkenKeep extends Structure {
	public static final MapCodec<SunkenKeep> CODEC = simpleCodec(SunkenKeep::new);
	// keeps everything within 8 chunks of the start chunk (structure reference range)
	static final int RADIUS = 40;
	static final int WALL_HEIGHT = 18;
	static final int DOME_HEIGHT = 32;
	public static final int LANDING_HEIGHT = 15;
	static final int ROCK_ABOVE = 8;
	static final int PORTAL = 8;
	static final int MAX_TUNNEL = 38;
	static final int BRIDGE = 24;

	public SunkenKeep(StructureSettings settings) {
		super(settings);
	}

	@Override
	public StructureType<?> type() {
		return Hollowmere.SUNKEN_KEEP;
	}

	record Plan(BlockPos portal, Direction dir, int floorY, int tunnel, long seed, BlockPos center) {
	}

	// deterministic per chunk (uses only the context's random) so WaysideForge, on the same placement grid, finds the same keep
	static Optional<Plan> plan(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		RandomSource random = context.random();
		int ex = chunk.getMiddleBlockX();
		int ez = chunk.getMiddleBlockZ();
		int surface = height(context, ex, ez, Heightmap.Types.WORLD_SURFACE_WG);
		if (surface <= context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		for (int dx = -9; dx <= 9; dx += 3) {
			for (int dz = -9; dz <= 9; dz += 3) {
				if (dx * dx + dz * dz > 81) {
					continue;
				}
				int solid = height(context, ex + dx, ez + dz, Heightmap.Types.OCEAN_FLOOR_WG);
				if (solid != height(context, ex + dx, ez + dz, Heightmap.Types.WORLD_SURFACE_WG) || Math.abs(solid - surface) > 3) {
					return Optional.empty();
				}
			}
		}
		List<Direction> ways = new java.util.ArrayList<>();
		java.util.Map<Direction, Double> climb = new java.util.EnumMap<>(Direction.class);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			double score = random.nextDouble() * 0.5;
			boolean open = true;
			for (int k = 6; k <= 18 && open; k += 4) {
				for (int side = -3; side <= 3; side += 3) {
					int fx = ex - dir.getStepX() * k - dir.getStepZ() * side;
					int fz = ez - dir.getStepZ() * k + dir.getStepX() * side;
					int front = height(context, fx, fz, Heightmap.Types.OCEAN_FLOOR_WG);
					if (front != height(context, fx, fz, Heightmap.Types.WORLD_SURFACE_WG) || front > surface + 2) {
						open = false;
						break;
					}
					int bx = ex + dir.getStepX() * k - dir.getStepZ() * side;
					int bz = ez + dir.getStepZ() * k + dir.getStepX() * side;
					score += height(context, bx, bz, Heightmap.Types.OCEAN_FLOOR_WG) - front;
				}
			}
			if (open) {
				ways.add(dir);
				climb.put(dir, score);
			}
		}
		ways.sort((a, b) -> Double.compare(climb.get(b), climb.get(a)));
		long seed = random.nextLong();
		for (Direction dir : ways) {
			int cx = ex + dir.getStepX() * (20 + RADIUS);
			int cz = ez + dir.getStepZ() * (20 + RADIUS);
			int lowest = surface;
			for (int i = 0; i < 16; i++) {
				double angle = i * Math.PI / 8;
				int reach = i % 2 == 0 ? RADIUS + 6 : RADIUS / 2;
				lowest = Math.min(lowest, height(context, cx + (int) (Math.cos(angle) * reach), cz + (int) (Math.sin(angle) * reach), Heightmap.Types.OCEAN_FLOOR_WG));
			}
			lowest = Math.min(lowest, height(context, cx, cz, Heightmap.Types.OCEAN_FLOOR_WG));
			int floorY = lowest - DOME_HEIGHT - ROCK_ABOVE;
			int tunnel = surface - 1 - (floorY + LANDING_HEIGHT);
			if (tunnel < 10 || tunnel > MAX_TUNNEL || floorY < context.heightAccessor().getMinY() + 12) {
				continue;
			}
			BlockPos portal = new BlockPos(ex, surface, ez);
			Cave cave = new Cave(0, 0, floorY, seed);
			double back = cave.wallRadius(Math.atan2(-dir.getStepZ(), -dir.getStepX()));
			BlockPos center = portal.relative(dir, tunnel + (int) back - 1).atY(floorY);
			return Optional.of(new Plan(portal, dir, floorY, tunnel, seed, center));
		}
		return Optional.empty();
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		// WaysideForge.site checks the forge's biomes, a superset of the keep's, so any keep that passes here gets its forge
		return plan(context).filter(plan -> WaysideForge.site(context, plan) != null).map(plan -> new GenerationStub(plan.portal(), builder -> {
			builder.addPiece(new CavernPiece(plan.center(), plan.floorY(), plan.dir(), plan.seed()));
			builder.addPiece(new TunnelPiece(plan.portal(), plan.dir(), plan.tunnel(), plan.center(), plan.floorY(), plan.seed()));
			builder.addPiece(new CastlePiece(context.structureTemplateManager(), plan.center(), plan.floorY(), plan.dir()));
		}));
	}

	static int height(GenerationContext context, int x, int z, Heightmap.Types type) {
		return context.chunkGenerator().getFirstOccupiedHeight(x, z, type, context.heightAccessor(), context.randomState());
	}

	static float hash(long seed, int x, int y, int z) {
		long h = Mth.getSeed(x, y, z) ^ seed * 0x9E3779B97F4A7C15L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h >>> 40) / (float) (1 << 24);
	}

	// the sign has no level during worldgen, so SignBlockEntity.setText can't be used; load the text as data instead
	static void writeSign(WorldGenLevel level, BlockPos pos, String keyPrefix) {
		if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
			List<Component> lines = java.util.stream.IntStream.rangeClosed(1, 4).mapToObj(i -> (Component) Component.translatable(keyPrefix + i)).toList();
			CompoundTag tag = new CompoundTag();
			net.minecraft.nbt.Tag text = SignText.CODEC.encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
				new SignText(lines, lines, DyeColor.BLACK, false)).getOrThrow();
			tag.put("front_text", text);
			tag.put("back_text", text.copy()); // hanging signs are read from both sides
			sign.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
		}
	}

	static boolean shapesToNeighbours(BlockState state) {
		return state.getBlock() instanceof CrossCollisionBlock || state.getBlock() instanceof net.minecraft.world.level.block.WallBlock
			|| state.getBlock() instanceof net.minecraft.world.level.block.StairBlock;
	}

	// shapes are fixed up when the proto chunk finishes; chunks that are already live (e.g. under /place) are skipped
	static void markShape(WorldGenLevel level, BlockPos pos, BlockState state) {
		if (shapesToNeighbours(state) && level.getChunk(pos) instanceof net.minecraft.world.level.chunk.ProtoChunk chunk
			&& !(chunk instanceof net.minecraft.world.level.chunk.ImposterProtoChunk)) {
			chunk.markPosForPostProcessing(pos);
		}
	}

	static void paintArms(WorldGenLevel level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BannerBlockEntity banner) {
			net.minecraft.nbt.ListTag patterns = new net.minecraft.nbt.ListTag();
			for (String[] layer : new String[][] {{"rhombus", "yellow"}, {"circle", "green"}, {"border", "black"}, {"curly_border", "yellow"}}) {
				CompoundTag entry = new CompoundTag();
				entry.putString("pattern", "minecraft:" + layer[0]);
				entry.putString("color", layer[1]);
				patterns.add(entry);
			}
			CompoundTag tag = new CompoundTag();
			tag.put("patterns", patterns);
			banner.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
		}
	}

	static final class Cave {
		final int cx;
		final int cz;
		final int floorY;
		final long seed;
		private final double[] phase = new double[12];

		Cave(int cx, int cz, int floorY, long seed) {
			this.cx = cx;
			this.cz = cz;
			this.floorY = floorY;
			this.seed = seed;
			RandomSource random = RandomSource.create(seed);
			for (int i = 0; i < this.phase.length; i++) {
				this.phase[i] = random.nextDouble() * Math.PI * 2;
			}
		}

		double wallRadius(double angle) {
			return RADIUS * (1 + 0.07 * Math.sin(3 * angle + this.phase[0]) + 0.04 * Math.sin(5 * angle + this.phase[1]) + 0.02 * Math.sin(11 * angle + this.phase[2]));
		}

		// 0 at the centre, 1 at the wall
		double r(int x, int z) {
			double dx = x - this.cx;
			double dz = z - this.cz;
			return Math.sqrt(dx * dx + dz * dz) / this.wallRadius(Math.atan2(dz, dx));
		}

		int floor(int x, int z) {
			double r = this.r(x, z);
			double edge = Math.max(0.0, (r - 0.8) / 0.2);
			double lumps = (1.3 * Math.sin(x * 0.13 + this.phase[3]) * Math.cos(z * 0.11 + this.phase[4]) + 0.9 * Math.sin((x + z) * 0.07 + this.phase[5]))
				* Mth.clamp((r - 0.62) / 0.15, 0.0, 1.0);
			return this.floorY + (int) Math.round(8 * edge * edge + lumps);
		}

		// highest air block (floor() is the top solid one), or below the floor when the column is solid rock
		int ceiling(int x, int z) {
			double r = this.r(x, z);
			if (r >= 1.0) {
				return Integer.MIN_VALUE;
			}
			double dome = WALL_HEIGHT + (DOME_HEIGHT - WALL_HEIGHT) * Math.sqrt(1 - r * r);
			double noise = 2.5 * Math.sin(x * 0.09 + this.phase[6]) * Math.sin(z * 0.1 + this.phase[7]) + 1.2 * Math.sin(x * 0.23 + z * 0.17 + this.phase[8]);
			return this.floorY + (int) Math.round(dome + noise);
		}

		boolean isAir(int x, int y, int z) {
			return y > this.floor(x, z) && y <= this.ceiling(x, z);
		}

		float hash(int x, int y, int z) {
			return SunkenKeep.hash(this.seed, x, y, z);
		}

		double phase(int i) {
			return this.phase[i];
		}
	}

	public static class CavernPiece extends StructurePiece {
		private final Cave cave;
		private final Direction dir;

		CavernPiece(BlockPos center, int floorY, Direction dir, long seed) {
			super(Hollowmere.KEEP_CAVERN, 0, new BoundingBox(center.getX() - RADIUS - 8, floorY - 4, center.getZ() - RADIUS - 8,
				center.getX() + RADIUS + 8, floorY + DOME_HEIGHT + 8, center.getZ() + RADIUS + 8));
			this.cave = new Cave(center.getX(), center.getZ(), floorY, seed);
			this.dir = dir;
		}

		public CavernPiece(CompoundTag tag) {
			super(Hollowmere.KEEP_CAVERN, tag);
			this.cave = new Cave(tag.getIntOr("CX", 0), tag.getIntOr("CZ", 0), tag.getIntOr("Floor", 0), tag.getLongOr("Seed", 0L));
			this.dir = Direction.from2DDataValue(tag.getIntOr("Dir", 0));
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			tag.putInt("CX", this.cave.cx);
			tag.putInt("CZ", this.cave.cz);
			tag.putInt("Floor", this.cave.floorY);
			tag.putLong("Seed", this.cave.seed);
			tag.putInt("Dir", this.dir.get2DDataValue());
		}

		private void set(WorldGenLevel level, BoundingBox bb, int x, int y, int z, BlockState state) {
			BlockPos pos = new BlockPos(x, y, z);
			if (bb.isInside(pos)) {
				level.setBlock(pos, state, 2);
				markShape(level, pos, state);
			}
		}

		private int along(int x, int z) {
			return (x - this.cave.cx) * this.dir.getStepX() + (z - this.cave.cz) * this.dir.getStepZ();
		}

		private int across(int x, int z) {
			return Math.abs((x - this.cave.cx) * this.dir.getStepZ() - (z - this.cave.cz) * this.dir.getStepX());
		}

		// deepslate at y 0 and below, blending in from y 8 like vanilla
		private BlockState rock(int x, int y, int z) {
			return y <= 0 || y < 8 && this.cave.hash(x, y, z) < (8 - y) / 8F ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
		}

		private boolean reserved(int x, int z) {
			boolean castle = Math.max(Math.abs(x - this.cave.cx), Math.abs(z - this.cave.cz)) <= 21;
			boolean approach = this.across(x, z) <= 4 && this.along(x, z) < 0;
			return castle || approach;
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos referencePos) {
			Cave cave = this.cave;
			int y0 = cave.floorY - 3;
			int y1 = cave.floorY + DOME_HEIGHT + 8;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = Math.max(bb.minX(), this.boundingBox.minX()); x <= Math.min(bb.maxX(), this.boundingBox.maxX()); x++) {
				for (int z = Math.max(bb.minZ(), this.boundingBox.minZ()); z <= Math.min(bb.maxZ(), this.boundingBox.maxZ()); z++) {
					double r = cave.r(x, z);
					if (r > 1.12) {
						continue;
					}
					int floor = cave.floor(x, z);
					int ceiling = cave.ceiling(x, z);
					int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
					// fill caves, fluids and loose ceiling blocks around the cavern with rock (below the surface only)
					for (int y = y0; y <= y1; y++) {
						pos.set(x, y, z);
						boolean shell = r > 1.0 || y <= floor || y > ceiling;
						if (shell) {
							BlockState state = level.getBlockState(pos);
							if (!state.getFluidState().isEmpty() || y < ground && state.canBeReplaced()
								|| y > ceiling && y <= ceiling + 3 && state.getBlock() instanceof FallingBlock) {
								level.setBlock(pos, this.rock(x, y, z), 2);
							}
						}
					}
					if (ceiling <= floor) {
						continue;
					}
					for (int y = floor + 1; y <= ceiling; y++) {
						level.setBlock(pos.set(x, y, z), Blocks.CAVE_AIR.defaultBlockState(), 2);
					}
					this.dressColumn(level, bb, x, z, floor, ceiling, r);
				}
			}
			this.placeBoulders(level, bb);
			this.placeTrees(level, bb);
			this.placePond(level, bb);
			this.placeLamps(level, bb);
		}

		private void dressColumn(WorldGenLevel level, BoundingBox bb, int x, int z, int floor, int ceiling, double r) {
			Cave cave = this.cave;
			float h = cave.hash(x, floor, z);
			double patch = Math.sin(x * 0.21 + cave.phase(9)) * Math.cos(z * 0.17 + cave.phase(10)) + h * 0.3;
			BlockState ground;
			if (r > 0.9) {
				ground = h < 0.3 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : h < 0.45 ? Blocks.GRAVEL.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
			} else if (patch > 0.75) {
				ground = Blocks.MOSS_BLOCK.defaultBlockState();
			} else {
				ground = h < 0.04 ? Blocks.COARSE_DIRT.defaultBlockState() : h < 0.06 ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
			}
			this.set(level, bb, x, floor, z, ground);
			this.set(level, bb, x, floor - 1, z, Blocks.DIRT.defaultBlockState());
			this.set(level, bb, x, floor - 2, z, h < 0.5 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());

			if (!this.reserved(x, z) && r < 0.97 && (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.MOSS_BLOCK)) && ceiling - floor > 3) {
				float p = cave.hash(x, floor + 1, z);
				BlockState plant = null;
				BlockState upper = null;
				if (p < 0.28F) {
					plant = Blocks.SHORT_GRASS.defaultBlockState();
				} else if (p < 0.33F) {
					plant = Blocks.TALL_GRASS.defaultBlockState();
					upper = plant.setValue(DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
				} else if (p < 0.37F) {
					plant = Blocks.FERN.defaultBlockState();
				} else if (p < 0.39F) {
					plant = Blocks.LARGE_FERN.defaultBlockState();
					upper = plant.setValue(DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
				} else if (p < 0.44F) {
					Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.ALLIUM};
					plant = flowers[(int) (cave.hash(x, floor + 7, z) * flowers.length)].defaultBlockState();
				} else if (p < 0.47F) {
					plant = Blocks.WILDFLOWERS.defaultBlockState().setValue(FlowerBedBlock.AMOUNT, 1 + (int) (cave.hash(x, floor + 3, z) * 4))
						.setValue(FlowerBedBlock.FACING, Direction.from2DDataValue((int) (cave.hash(x, floor + 5, z) * 4)));
				} else if (p < 0.49F) {
					plant = (p < 0.48F ? Blocks.AZALEA : Blocks.FLOWERING_AZALEA).defaultBlockState();
				} else if (p < 0.52F) {
					plant = Blocks.MOSS_CARPET.defaultBlockState();
				} else if (p < 0.525F) {
					plant = Blocks.FIREFLY_BUSH.defaultBlockState();
				} else if (p < 0.535F) {
					plant = Blocks.BUSH.defaultBlockState();
				}
				if (plant != null) {
					this.set(level, bb, x, floor + 1, z, plant);
					if (upper != null) {
						this.set(level, bb, x, floor + 2, z, upper);
					}
				}
			}

			float c = cave.hash(x, ceiling, z);
			int room = ceiling - floor - 3;
			if (c < 0.012F) {
				this.set(level, bb, x, ceiling, z, Hollowmere.SUNSTONE_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN));
			} else if (c < 0.04F && room > 2) {
				int length = 1 + (int) (cave.hash(x, ceiling - 1, z) * Math.min(7, room));
				for (int i = 0; i < length; i++) {
					boolean tip = i == length - 1;
					BlockState vine = (tip ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
						.setValue(CaveVines.BERRIES, cave.hash(x, ceiling - i, z + 1) < 0.35F);
					this.set(level, bb, x, ceiling - i, z, tip ? vine.setValue(GrowingPlantHeadBlock.AGE, GrowingPlantHeadBlock.MAX_AGE) : vine);
				}
			} else if (c < 0.055F) {
				this.set(level, bb, x, ceiling, z, Blocks.HANGING_ROOTS.defaultBlockState());
			} else if (c < 0.057F) {
				this.set(level, bb, x, ceiling, z, Blocks.SPORE_BLOSSOM.defaultBlockState());
			}

			if (r > 0.85) {
				for (int y = floor + 2; y < ceiling - 1; y++) {
					for (Direction side : Direction.Plane.HORIZONTAL) {
						if (!cave.isAir(x + side.getStepX(), y, z + side.getStepZ())) {
							float w = cave.hash(x, y, z + 31 * side.get2DDataValue());
							if (w < 0.05F) {
								this.set(level, bb, x, y, z, Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(side), true));
							} else if (w < 0.13F) {
								this.set(level, bb, x, y, z, Blocks.VINE.defaultBlockState().setValue(VineBlock.PROPERTY_BY_DIRECTION.get(side), true));
							}
							break;
						}
					}
				}
			}
		}

		private void placeBoulders(WorldGenLevel level, BoundingBox bb) {
			Cave cave = this.cave;
			for (int gx = -5; gx <= 5; gx++) {
				for (int gz = -5; gz <= 5; gz++) {
					int bx = cave.cx + gx * 8 + (int) (cave.hash(gx, 1, gz) * 5) - 2;
					int bz = cave.cz + gz * 8 + (int) (cave.hash(gx, 2, gz) * 5) - 2;
					double r = cave.r(bx, bz);
					if (r > 0.85 || r < 0.2 || this.reserved(bx, bz) || !this.boundingBox.isInside(bx, cave.floorY, bz)) {
						continue;
					}
					int base = cave.floor(bx, bz);
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							boolean corner = dx != 0 && dz != 0;
							if (corner && cave.hash(bx + dx, base, bz + dz) < 0.5F) {
								continue;
							}
							int top = dx == 0 && dz == 0 ? 2 : 1;
							for (int y = 1; y <= top; y++) {
								float m = cave.hash(bx + dx, base + y, bz + dz);
								this.set(level, bb, bx + dx, base + y, bz + dz, (m < 0.4F ? Blocks.MOSSY_COBBLESTONE : m < 0.7F ? Blocks.TUFF : Blocks.MOSS_BLOCK).defaultBlockState());
							}
							if (!corner && (dx != 0 || dz != 0) && cave.hash(bx + dx, base + 9, bz + dz) < 0.5F) {
								this.set(level, bb, bx + dx, base + 2, bz + dz, Hollowmere.SUNSTONE_CLUSTER.defaultBlockState());
							}
						}
					}
					this.set(level, bb, bx, base + 3, bz, Hollowmere.SUNSTONE_CLUSTER.defaultBlockState());
				}
			}
		}

		private void placeTrees(WorldGenLevel level, BoundingBox bb) {
			Cave cave = this.cave;
			RandomSource random = RandomSource.create(cave.seed ^ 0x7EEL);
			for (int i = 0; i < 9; i++) {
				double angle = random.nextDouble() * Math.PI * 2;
				double distance = 28 + random.nextDouble() * 8;
				int tx = cave.cx + (int) (Math.cos(angle) * distance);
				int tz = cave.cz + (int) (Math.sin(angle) * distance);
				int trunk = 4 + random.nextInt(3);
				if (this.reserved(tx, tz) || cave.r(tx, tz) > 0.86) {
					continue;
				}
				int base = cave.floor(tx, tz);
				for (int y = 1; y <= trunk; y++) {
					this.set(level, bb, tx, base + y, tz, Blocks.OAK_LOG.defaultBlockState());
				}
				for (int dx = -3; dx <= 3; dx++) {
					for (int dy = -2; dy <= 2; dy++) {
						for (int dz = -3; dz <= 3; dz++) {
							double d = dx * dx + dz * dz + dy * dy * 2.2;
							int x = tx + dx;
							int y = base + trunk + dy;
							int z = tz + dz;
							if (d > 9.5 || cave.hash(x, y, z) < 0.12F || !cave.isAir(x, y, z) || dx == 0 && dz == 0 && dy <= 0) {
								continue;
							}
							Block leaves = cave.hash(x, y + 1, z) < 0.3F ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES;
							this.set(level, bb, x, y, z, leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
						}
					}
				}
			}
		}

		private void placePond(WorldGenLevel level, BoundingBox bb) {
			Cave cave = this.cave;
			Direction side = cave.hash(0, 0, 0) < 0.5F ? this.dir.getClockWise() : this.dir.getCounterClockWise();
			int px = cave.cx + side.getStepX() * 27 + this.dir.getStepX() * 6;
			int pz = cave.cz + side.getStepZ() * 27 + this.dir.getStepZ() * 6;
			int water = cave.floorY;
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					int x = px + dx;
					int z = pz + dz;
					boolean alongX = side.getAxis() == Direction.Axis.X;
					double d = Math.sqrt((alongX ? dx * dx / 16.0 + dz * dz / 25.0 : dx * dx / 25.0 + dz * dz / 16.0));
					int ground = cave.floor(x, z);
					float h = cave.hash(x, 77, z);
					if (d <= 1.0) {
						// water sits at a single level dug into the slope, so it never flows
						for (int y = water + 1; y <= ground; y++) {
							this.set(level, bb, x, y, z, Blocks.CAVE_AIR.defaultBlockState());
						}
						this.set(level, bb, x, water, z, Blocks.WATER.defaultBlockState());
						this.set(level, bb, x, water - 1, z, d < 0.6 ? Blocks.WATER.defaultBlockState() : Blocks.CLAY.defaultBlockState());
						this.set(level, bb, x, water - 2, z, Blocks.CLAY.defaultBlockState());
						this.set(level, bb, x, water + 1, z, h < 0.15F ? Blocks.LILY_PAD.defaultBlockState() : Blocks.CAVE_AIR.defaultBlockState());
					} else if (d <= 1.4) {
						if (ground < water) {
							this.set(level, bb, x, water, z, Blocks.GRASS_BLOCK.defaultBlockState());
							ground = water;
						}
						BlockState rim = h < 0.18F ? Blocks.SUGAR_CANE.defaultBlockState() : h < 0.3F
							? Blocks.BIG_DRIPLEAF.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue((int) (h * 40) & 3))
							: h < 0.4F ? Blocks.FIREFLY_BUSH.defaultBlockState() : null;
						if (rim != null) {
							this.set(level, bb, x, ground, z, Blocks.GRASS_BLOCK.defaultBlockState());
							this.set(level, bb, x, ground + 1, z, rim);
							if (rim.is(Blocks.SUGAR_CANE)) {
								this.set(level, bb, x, ground + 2, z, rim);
							}
						}
					}
				}
			}
			BlockPos shore = new BlockPos(px - this.dir.getStepX() * 6, Math.max(water, cave.floor(px - this.dir.getStepX() * 6, pz - this.dir.getStepZ() * 6)) + 1,
				pz - this.dir.getStepZ() * 6);
			if (bb.isInside(shore)) {
				MimicEntity mimic = Mimicry.MIMIC.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (mimic != null) {
					mimic.snapTo(Vec3.atBottomCenterOf(shore), this.dir.toYRot(), 0.0F);
					mimic.setYBodyRot(this.dir.toYRot());
					mimic.setYHeadRot(this.dir.toYRot());
					mimic.setDormant(true);
					mimic.setPersistenceRequired();
					level.addFreshEntity(mimic);
				}
			}
		}

		private void placeLamps(WorldGenLevel level, BoundingBox bb) {
			Cave cave = this.cave;
			for (int along : new int[] {-30, -24}) {
				for (int sign : new int[] {-1, 1}) {
					int x = cave.cx + this.dir.getStepX() * along + this.dir.getStepZ() * 3 * sign;
					int z = cave.cz + this.dir.getStepZ() * along - this.dir.getStepX() * 3 * sign;
					int base = cave.floor(x, z);
					for (int y = 1; y <= 2; y++) {
						this.set(level, bb, x, base + y, z, Blocks.SPRUCE_FENCE.defaultBlockState());
					}
					this.set(level, bb, x, base + 3, z, Hollowmere.SUNSTONE_LANTERN.defaultBlockState());
				}
			}
		}
	}

	public static class TunnelPiece extends StructurePiece {
		private final int surface;
		private final int tunnel;
		private final Cave cave;

		TunnelPiece(BlockPos portal, Direction dir, int tunnel, BlockPos center, int floorY, long seed) {
			super(Hollowmere.KEEP_TUNNEL, 0, makeBoundingBox(0, floorY - 3, 0, dir, 15, portal.getY() + 12 - (floorY - 3), PORTAL + tunnel + 3 + BRIDGE));
			this.setOrientation(dir);
			BlockPos at = this.getWorldPos(7, 0, PORTAL);
			this.boundingBox = this.boundingBox.moved(portal.getX() - at.getX(), 0, portal.getZ() - at.getZ());
			this.surface = portal.getY();
			this.tunnel = tunnel;
			this.cave = new Cave(center.getX(), center.getZ(), floorY, seed);
		}

		public TunnelPiece(CompoundTag tag) {
			super(Hollowmere.KEEP_TUNNEL, tag);
			this.surface = tag.getIntOr("Surface", 0);
			this.tunnel = tag.getIntOr("Tunnel", 0);
			this.cave = new Cave(tag.getIntOr("CX", 0), tag.getIntOr("CZ", 0), tag.getIntOr("Floor", 0), tag.getLongOr("Seed", 0L));
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			tag.putInt("Surface", this.surface);
			tag.putInt("Tunnel", this.tunnel);
			tag.putInt("CX", this.cave.cx);
			tag.putInt("CZ", this.cave.cz);
			tag.putInt("Floor", this.cave.floorY);
			tag.putLong("Seed", this.cave.seed);
		}

		private int ly(int worldY) {
			return worldY - this.boundingBox.minY();
		}

		private void put(WorldGenLevel level, BoundingBox bb, int x, int worldY, int z, BlockState state) {
			// authored with +z = south; vanilla oriented pieces use +z = north, so mirror across Z
			state = state.mirror(net.minecraft.world.level.block.Mirror.LEFT_RIGHT);
			this.placeBlock(level, state, x, this.ly(worldY), z, bb);
			BlockPos pos = this.getWorldPos(x, this.ly(worldY), z);
			if (bb.isInside(pos)) {
				markShape(level, pos, state);
			}
		}

		private BlockState get(WorldGenLevel level, BoundingBox bb, int x, int worldY, int z) {
			return this.getBlock(level, x, this.ly(worldY), z, bb);
		}

		private float hash(int x, int worldY, int z) {
			BlockPos pos = this.getWorldPos(x, this.ly(worldY), z);
			return this.cave.hash(pos.getX(), pos.getY(), pos.getZ());
		}

		public BlockPos portal() {
			return this.getWorldPos(7, this.ly(this.surface), PORTAL);
		}

		// world y of the top sturdy non-log, non-leaf block; only valid for columns in the current chunk
		private int groundAt(WorldGenLevel level, int x, int z) {
			BlockPos column = this.getWorldPos(x, 0, z);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(column.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, column.getX(),
				column.getZ()) - 1, column.getZ());
			while (pos.getY() > level.getMinY()) {
				BlockState state = level.getBlockState(pos);
				if (state.isFaceSturdy(level, pos, Direction.UP) && !state.is(net.minecraft.tags.BlockTags.LOGS) && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
					break;
				}
				pos.move(Direction.DOWN);
			}
			return pos.getY();
		}

		// scans bottom up so the upper half of a tall plant is removed after its base
		private void tidyUp(WorldGenLevel level, BoundingBox bb) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int z = 0; z < PORTAL + 10; z++) {
				for (int x = 0; x < 15; x++) {
					BlockPos column = this.getWorldPos(x, 0, z);
					if (!bb.isInside(column.getX(), bb.minY(), column.getZ())) {
						continue;
					}
					for (int y = this.surface - 8; y <= this.surface + 10; y++) {
						BlockState state = level.getBlockState(pos.set(column.getX(), y, column.getZ()));
						if (!state.isAir() && state.getFluidState().isEmpty() && !state.canSurvive(level, pos)) {
							level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			}
		}

		private void uproot(WorldGenLevel level, BoundingBox bb, int x, int ground, int z) {
			BlockPos above = this.getWorldPos(x, this.ly(ground + 1), z);
			if (!bb.isInside(above)) {
				return;
			}
			BlockState plant = level.getBlockState(above);
			if (!plant.isAir() && plant.getFluidState().isEmpty() && !plant.canSurvive(level, above)) {
				level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
				if (plant.getBlock() instanceof DoublePlantBlock) {
					level.setBlock(above.above(), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}

		private boolean inChunk(BoundingBox bb, int x, int z) {
			BlockPos pos = this.getWorldPos(x, 0, z);
			return pos.getX() >= bb.minX() && pos.getX() <= bb.maxX() && pos.getZ() >= bb.minZ() && pos.getZ() <= bb.maxZ();
		}

		private void onGround(WorldGenLevel level, BoundingBox bb, int x, int dy, int z, BlockState state) {
			if (this.inChunk(bb, x, z)) {
				this.put(level, bb, x, this.groundAt(level, x, z) + dy, z, state);
			}
		}

		private int caveFloorAt(int x, int z) {
			BlockPos pos = this.getWorldPos(x, 0, z);
			return this.cave.floor(pos.getX(), pos.getZ());
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos referencePos) {
			this.buildYard(level, bb);
			this.buildShaft(level, bb);
			this.buildBalconyAndBridge(level, bb);
			this.tidyUp(level, bb);
		}

		private void buildYard(WorldGenLevel level, BoundingBox bb) {
			for (int z = 0; z < PORTAL + 7; z++) {
				for (int x = 0; x < 15; x++) {
					if (!this.inChunk(bb, x, z)) {
						continue;
					}
					int ground = this.groundAt(level, x, z);
					boolean shaft = x >= 5 && x <= 9 && z >= PORTAL;
					double spread = Math.hypot(x - 7, (z - PORTAL) * 1.3);
					if (spread > 8.5) {
						continue;
					}
					for (int y = ground + 1; y <= ground + 14 && x >= 4 && x <= 10 && z >= PORTAL - 1; y++) {
						BlockState above = this.get(level, bb, x, y, z);
						if (above.is(net.minecraft.tags.BlockTags.LOGS) || above.is(net.minecraft.tags.BlockTags.LEAVES) || above.is(Blocks.VINE)) {
							this.put(level, bb, x, y, z, Blocks.AIR.defaultBlockState());
						} else {
							this.uproot(level, bb, x, y - 1, z);
						}
					}
					if (shaft) {
						continue;
					}
					BlockState top = this.get(level, bb, x, ground, z);
					if (top.isAir()) {
						ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.getWorldPos(x, 0, z).getX(), this.getWorldPos(x, 0, z).getZ()) - 1;
					}
					float h = this.hash(x, ground, z);
					if (h < 0.35F) {
						this.put(level, bb, x, ground, z, (h < 0.12F ? Blocks.GRAVEL : h < 0.24F ? Blocks.COARSE_DIRT : Blocks.COBBLESTONE).defaultBlockState());
						this.uproot(level, bb, x, ground, z);
					}
					float k = this.hash(x, ground + 1, z);
					if (x == 8 && z < PORTAL) {
						this.put(level, bb, x, ground + 1, z, k < 0.3F ? Blocks.AIR.defaultBlockState()
							: Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
					} else if (k < 0.06F) {
						this.put(level, bb, x, ground + 1, z, (k < 0.03F ? Blocks.COBBLESTONE : Blocks.ANDESITE).defaultBlockState());
						if (k < 0.015F) {
							this.put(level, bb, x, ground + 2, z, Blocks.COBBLESTONE_SLAB.defaultBlockState());
						}
					} else if (k < 0.09F) {
						this.put(level, bb, x, ground + 1, z, Blocks.COBWEB.defaultBlockState());
					} else if (k < 0.12F) {
						this.put(level, bb, x, ground + 1, z, Blocks.GRAVEL.defaultBlockState());
					}
				}
			}
			int portalY = this.surface;
			this.onGround(level, bb, 3, 1, 5, Blocks.BARREL.defaultBlockState());
			this.onGround(level, bb, 3, 2, 5, Blocks.SPRUCE_SLAB.defaultBlockState());
			this.onGround(level, bb, 2, 1, 6, Blocks.BARREL.defaultBlockState());
			this.onGround(level, bb, 11, 1, 4, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
			this.onGround(level, bb, 12, 1, 7, Blocks.SPRUCE_FENCE.defaultBlockState());
			this.onGround(level, bb, 12, 2, 7, Blocks.SPRUCE_FENCE.defaultBlockState());
			this.onGround(level, bb, 12, 3, 7, Blocks.LANTERN.defaultBlockState());
			BlockState post = Blocks.DARK_OAK_LOG.defaultBlockState();
			BlockState beam = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
			for (int y = portalY; y <= portalY + 3; y++) {
				this.put(level, bb, 5, y, PORTAL, post);
				this.put(level, bb, 9, y, PORTAL, post);
			}
			for (int x = 4; x <= 10; x++) {
				this.put(level, bb, x, portalY + 4, PORTAL, beam);
				this.put(level, bb, x, portalY + 4, PORTAL + 1, Blocks.SPRUCE_SLAB.defaultBlockState());
				this.put(level, bb, x, portalY + 5, PORTAL, Blocks.SPRUCE_SLAB.defaultBlockState());
			}
			this.put(level, bb, 4, portalY + 3, PORTAL, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
			this.put(level, bb, 10, portalY + 3, PORTAL, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
			this.put(level, bb, 7, portalY + 4, PORTAL - 1, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH));
			BlockPos sign = this.getWorldPos(7, this.ly(portalY + 4), PORTAL - 1);
			if (bb.isInside(sign)) {
				writeSign(level, sign, "sign.mimicry.keep.");
			}
			if (this.inChunk(bb, 8, 3)) {
				BlockPos cart = this.getWorldPos(8, this.ly(this.groundAt(level, 8, 3) + 1), 3);
				Minecart minecart = EntityTypes.MINECART.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (minecart != null) {
					minecart.snapTo(Vec3.atBottomCenterOf(cart));
					level.addFreshEntity(minecart);
				}
			}
		}

		private void buildShaft(WorldGenLevel level, BoundingBox bb) {
			BlockState stair = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
			BlockState mossyStair = Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
			BlockState air = Blocks.AIR.defaultBlockState();
			for (int i = 0; i < this.tunnel; i++) {
				int z = PORTAL + i;
				int y = this.surface - 1 - i;
				int top = i < 6 && this.inChunk(bb, 7, z) ? Math.max(y + 4, this.groundAt(level, 7, z)) : y + 4;
				for (int x = 6; x <= 8; x++) {
					for (int ay = y + 1; ay <= top; ay++) {
						this.put(level, bb, x, ay, z, air);
					}
				}
				this.put(level, bb, 6, y, z, this.hash(6, y, z) < 0.4F ? mossyStair : stair);
				this.put(level, bb, 7, y, z, this.hash(7, y, z) < 0.4F ? mossyStair : stair);
				this.put(level, bb, 8, y, z, Blocks.COBBLESTONE.defaultBlockState());
				boolean flat = i == 0 || i == this.tunnel - 1;
				if (this.hash(8, y + 1, z) > 0.15F) {
					this.put(level, bb, 8, y + 1, z, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, flat ? RailShape.NORTH_SOUTH : RailShape.ASCENDING_NORTH));
				}
				for (int wy = y; wy <= y + 5; wy++) {
					for (int x : new int[] {5, 9}) {
						BlockState wall = this.get(level, bb, x, wy, z);
						if (wall.isAir() || !wall.getFluidState().isEmpty() || wall.getBlock() instanceof FallingBlock) {
							this.put(level, bb, x, wy, z, (this.hash(x, wy, z) < 0.3F ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState());
						}
					}
				}
				for (int x = 5; x <= 9; x++) {
					BlockState ceiling = this.get(level, bb, x, y + 5, z);
					if (i >= 5 && (!ceiling.getFluidState().isEmpty() || ceiling.getBlock() instanceof FallingBlock || ceiling.isAir())) {
						this.put(level, bb, x, y + 5, z, Blocks.COBBLESTONE.defaultBlockState());
					}
					BlockState under = this.get(level, bb, x, y - 1, z);
					if (!under.getFluidState().isEmpty() || under.isAir()) {
						this.put(level, bb, x, y - 1, z, Blocks.COBBLESTONE.defaultBlockState());
					}
				}
				if (i % 4 == 2 && i > 3) {
					for (int py = y + 1; py <= y + 4; py++) {
						this.put(level, bb, 5, py, z, Blocks.SPRUCE_LOG.defaultBlockState());
						this.put(level, bb, 9, py, z, Blocks.SPRUCE_LOG.defaultBlockState());
					}
					for (int x = 5; x <= 9; x++) {
						this.put(level, bb, x, y + 5, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
					}
					if (i % 8 == 6 && this.hash(7, y + 4, z) < 0.7F) {
						this.put(level, bb, 7, y + 4, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
					}
				}
				if (this.hash(6, y + 4, z) < 0.12F) {
					this.put(level, bb, 6, y + 4, z, Blocks.COBWEB.defaultBlockState());
				}
			}
		}

		private void buildBalconyAndBridge(WorldGenLevel level, BoundingBox bb) {
			int landing = this.surface - 1 - this.tunnel;
			int z0 = PORTAL + this.tunnel;
			BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
			BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
			BlockState lantern = Hollowmere.SUNSTONE_LANTERN.defaultBlockState();
			for (int z = z0; z <= z0 + 2; z++) {
				for (int x = 5; x <= 9; x++) {
					this.put(level, bb, x, landing, z, planks);
					for (int y = landing + 1; y <= landing + 4; y++) {
						this.put(level, bb, x, y, z, Blocks.AIR.defaultBlockState());
					}
				}
				this.put(level, bb, 5, landing + 1, z, fence);
				this.put(level, bb, 9, landing + 1, z, fence);
			}
			this.put(level, bb, 5, landing + 2, z0 + 2, lantern);
			this.put(level, bb, 9, landing + 2, z0 + 2, lantern);
			for (int x : new int[] {5, 9}) {
				for (int y = landing - 1; y > this.caveFloorAt(x, z0 + 2); y--) {
					this.put(level, bb, x, y, z0 + 2, Blocks.SPRUCE_LOG.defaultBlockState());
				}
			}

			BlockState step = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
			boolean grounded = false;
			for (int j = 0; j < BRIDGE; j++) {
				int z = z0 + 3 + j;
				int y = landing - 1 - j;
				int ground = Math.max(this.caveFloorAt(6, z), Math.max(this.caveFloorAt(7, z), this.caveFloorAt(8, z)));
				if (grounded || y <= ground) {
					grounded = true;
					for (int x = 6; x <= 8; x++) {
						int g = this.caveFloorAt(x, z);
						float h = this.hash(x, g, z);
						this.put(level, bb, x, g, z, (h < 0.55F ? Blocks.DIRT_PATH : h < 0.8F ? Blocks.COARSE_DIRT : Blocks.GRAVEL).defaultBlockState());
						this.put(level, bb, x, g + 1, z, Blocks.CAVE_AIR.defaultBlockState());
					}
					continue;
				}
				for (int x = 6; x <= 8; x++) {
					this.put(level, bb, x, y, z, step);
					for (int ay = y + 1; ay <= y + 4; ay++) {
						this.put(level, bb, x, ay, z, Blocks.CAVE_AIR.defaultBlockState());
					}
				}
				this.put(level, bb, 5, y + 1, z, fence);
				this.put(level, bb, 9, y + 1, z, fence);
				this.put(level, bb, 5, y, z, planks);
				this.put(level, bb, 9, y, z, planks);
				if (j % 4 == 3) {
					for (int x : new int[] {5, 9}) {
						for (int py = y - 1; py > this.caveFloorAt(x, z); py--) {
							this.put(level, bb, x, py, z, Blocks.SPRUCE_LOG.defaultBlockState());
						}
					}
				}
				if (j % 6 == 5) {
					this.put(level, bb, j % 12 == 5 ? 5 : 9, y + 2, z, lantern);
				}
			}
		}
	}
}

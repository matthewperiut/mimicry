package com.slainlight.mimicry;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// shapes forge ground at the noise stage (as a Beardifier) so surface rules and decoration run on the final terrain
public final class Earthworks extends Beardifier {
	// margin around the forge piece kept free of trunks; crowns can still reach into it
	static final int GLADE = 4;
	// half-width of the clear area around the mine portal, including a margin for canopies
	static final int YARD = 13;
	// far beyond terrain noise density, which stays within about +/-0.5 even in caves
	private static final float FORCE = 100.0F;
	// blocks shaped past the old and new ground, to settle overhangs and near-surface caves
	private static final int SKIN = 5;
	// the chunk plus one column each side, in case the volume is sampled at its edges
	private static final int SPAN = 18;

	private static final ThreadLocal<List<BoundingBox>> CLEAR = new ThreadLocal<>();

	private final Beardifier vanilla;
	private final int minX;
	private final int minZ;
	private final int[] surface = new int[SPAN * SPAN];
	private final int[] low = new int[SPAN * SPAN];
	private final int[] high = new int[SPAN * SPAN];

	private Earthworks(Beardifier vanilla, List<WaysideForge.Grounds> grounds, ChunkAccess chunk, NoiseBasedChunkGenerator generator,
		RandomState randomState) {
		super(List.of(), List.of(), null);
		this.vanilla = vanilla;
		this.minX = chunk.getPos().getMinBlockX() - 1;
		this.minZ = chunk.getPos().getMinBlockZ() - 1;
		for (int i = 0; i < SPAN * SPAN; i++) {
			int x = this.minX + i % SPAN;
			int z = this.minZ + i / SPAN;
			this.low[i] = 1;
			this.high[i] = 0;
			WaysideForge.Grounds nearest = null;
			double t = 1.0;
			for (WaysideForge.Grounds g : grounds) {
				double gt = g.blend(x, z);
				if (gt < t) {
					t = gt;
					nearest = g;
				}
			}
			if (nearest == null) {
				continue;
			}
			int natural = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, chunk, randomState) - 1;
			double ease = t * t * (3 - 2 * t);
			int target = (int) Math.round(Mth.lerp(ease, nearest.floor(), natural));
			if (target == natural && t > 0) {
				continue;
			}
			this.surface[i] = target;
			this.low[i] = Math.min(target, natural) - SKIN;
			this.high[i] = Math.max(target, natural) + SKIN;
		}
	}

	public static Beardifier around(Beardifier vanilla, StructureManager structures, ChunkAccess chunk, NoiseBasedChunkGenerator generator,
		RandomState randomState) {
		ChunkPos pos = chunk.getPos();
		List<WaysideForge.Grounds> grounds = structures.startsForStructure(pos.x(), pos.z(), s -> s instanceof WaysideForge).stream()
			.flatMap(start -> start.getPieces().stream())
			.filter(piece -> piece instanceof WaysideForge.Grounds && piece.getBoundingBox().intersects(pos.getMinBlockX() - 1, pos.getMinBlockZ() - 1,
				pos.getMaxBlockX() + 1, pos.getMaxBlockZ() + 1))
			.map(piece -> (WaysideForge.Grounds) piece).toList();
		return grounds.isEmpty() ? vanilla : new Earthworks(vanilla, grounds, chunk, generator, randomState);
	}

	private float shape(int x, int y, int z) {
		int dx = x - this.minX;
		int dz = z - this.minZ;
		if (dx < 0 || dx >= SPAN || dz < 0 || dz >= SPAN) {
			return 0.0F;
		}
		int i = dx + dz * SPAN;
		if (y < this.low[i] || y > this.high[i]) {
			return 0.0F;
		}
		return y <= this.surface[i] ? FORCE : -FORCE;
	}

	@Override
	public float sampleValue(SamplerContext context, int x, int y, int z) {
		return this.vanilla.sampleValue(context, x, y, z) + this.shape(x, y, z);
	}

	@Override
	public void sampleVolume(SamplerContext context, DensityBuffer output, DensityVolume volume) {
		this.vanilla.sampleVolume(context, output, volume);
		int i = 0;
		for (int z = 0; z < volume.sizeZ(); z++) {
			int blockZ = volume.blockZ(z);
			for (int x = 0; x < volume.sizeX(); x++) {
				int blockX = volume.blockX(x);
				for (int y = 0; y < volume.sizeY(); y++) {
					float shape = this.shape(blockX, volume.blockY(y), blockZ);
					if (shape != 0.0F) {
						output.addTo(i, shape);
					}
					i++;
				}
			}
		}
	}

	public static void beginDecorating(StructureManager structures, ChunkPos pos) {
		List<BoundingBox> clear = structures.startsForStructure(pos.x(), pos.z(), s -> s instanceof WaysideForge || s instanceof SunkenKeep).stream()
			.flatMap(start -> start.getPieces().stream())
			.map(piece -> piece instanceof WaysideForge.Piece forge ? forge.getBoundingBox().inflatedBy(GLADE, 0, GLADE)
				: piece instanceof SunkenKeep.TunnelPiece tunnel ? BoundingBox.fromCorners(tunnel.portal().offset(-YARD, 0, -YARD), tunnel.portal().offset(YARD, 0, YARD))
				: null)
			.filter(box -> box != null).toList();
		CLEAR.set(clear.isEmpty() ? null : clear);
	}

	public static void endDecorating() {
		CLEAR.remove();
	}

	public static boolean keepsClear(Feature feature, BlockPos origin) {
		List<BoundingBox> clear = CLEAR.get();
		if (clear == null || !(feature instanceof TreeFeature || feature instanceof FallenTreeFeature || feature instanceof BlockBlobFeature
			|| feature instanceof AbstractHugeMushroomFeature || feature instanceof HugeFungusFeature || feature instanceof LakeFeature)) {
			return false;
		}
		for (BoundingBox box : clear) {
			if (origin.getX() >= box.minX() && origin.getX() <= box.maxX() && origin.getZ() >= box.minZ() && origin.getZ() <= box.maxZ()) {
				return true;
			}
		}
		return false;
	}
}

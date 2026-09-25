package com.slainlight.mimicry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if <1.21.5 {
/*import net.minecraft.world.level.Level;
*///?}
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class TreasureLensItem extends Item {
	private static final int RANGE = 24;

	public TreasureLensItem(Item.Properties properties) {
		super(properties);
	}

	//? if >=1.21.5 {
	@Override
	public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (slot == null || slot.getType() != EquipmentSlot.Type.HAND || !(owner instanceof ServerPlayer player) || level.getGameTime() % 20 != 0) {
			return;
		}
	//?} else {
	/*@Override
	public void inventoryTick(ItemStack itemStack, Level world, Entity owner, int slot, boolean selected) {
		if (!(world instanceof ServerLevel level) || !(owner instanceof ServerPlayer player) || level.getGameTime() % 20 != 0
			|| player.getMainHandItem() != itemStack && player.getOffhandItem() != itemStack) {
			return;
		}
	*///?}

		BlockPos center = player.blockPosition();
		int chunkRange = (RANGE >> 4) + 1;
		for (int cx = -chunkRange; cx <= chunkRange; cx++) {
			for (int cz = -chunkRange; cz <= chunkRange; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow((center.getX() >> 4) + cx, (center.getZ() >> 4) + cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					BlockPos pos = blockEntity.getBlockPos();
					if (blockEntity instanceof ChestBlockEntity chest && pos.closerThan(center, RANGE) && /*? if >=1.20.5 {*/chest.getLootTable() != null/*?} else {*//*chest.saveWithoutMetadata().contains("LootTable")*//*?}*/) {
						mark(level, player, Mimicry.isMimic(level, pos, chest.getBlockState()) ? ParticleTypes.ANGRY_VILLAGER : ParticleTypes.WAX_ON,
							pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5);
					}
				}
			}
		}
		for (MimicEntity mimic : level.getEntitiesOfClass(MimicEntity.class, player.getBoundingBox().inflate(RANGE), mimic -> !mimic.isTame())) {
			mark(level, player, ParticleTypes.ANGRY_VILLAGER, mimic.getX(), mimic.getY() + 1.1, mimic.getZ());
		}
	}

	private static void mark(ServerLevel level, ServerPlayer player, SimpleParticleType particle, double x, double y, double z) {
		level.sendParticles(player, particle, true, /*? if >=1.21.4 {*/true, /*?}*/x, y, z, 3, 0.25, 0.2, 0.25, 0.0);
	}
}

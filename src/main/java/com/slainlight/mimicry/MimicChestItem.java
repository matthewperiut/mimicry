package com.slainlight.mimicry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.Nullable;
//? if <1.20.5 {
/*import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
*///?}

// always placed as a single chest: a double chest is never a mimic
public class MimicChestItem extends BlockItem {
	public MimicChestItem(Properties properties) {
		super(Blocks.CHEST, properties);
	}

	@Override
	protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
		BlockState state = super.getPlacementState(context);
		return state == null ? null : state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
	}

	//? if <26.1 {
	/*// block items take the block's name here, which would be plain "Chest"
	@Override
	public String getDescriptionId() {
		return this.getOrCreateDescriptionId();
	}
	*///?}

	//? if <1.20.5 {
	/*// no default item data here, so every stack without block entity data places a chest with the mimic loot table
	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
		if (getBlockEntityData(stack) == null && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
			chest.setLootTable(Mimicry.MIMIC_CHEST_LOOT, 0L);
		}
		return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
	}
	*///?}
}

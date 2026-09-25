package com.slainlight.mimicry.mixin;

import com.slainlight.mimicry.KeepLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

// players walk through keep containers that are hidden from them
@Mixin({ChestBlock.class, BarrelBlock.class})
public abstract class KeepContainerMixin extends BaseEntityBlock {
	protected KeepContainerMixin(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return KeepLoot.passesThrough(pos, context) ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
	}
}

package com.slainlight.mimicry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
//? if >=1.21.2 {
import net.minecraft.world.level.ScheduledTickAccess;
//?} else {
/*import net.minecraft.world.level.LevelAccessor;
*///?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class ChimneyBlock extends Block implements EntityBlock {
	public static final BooleanProperty SIGNAL_FIRE = CampfireBlock.SIGNAL_FIRE;

	public ChimneyBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(SIGNAL_FIRE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SIGNAL_FIRE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(SIGNAL_FIRE, context.getLevel().getBlockState(context.getClickedPos().below()).is(Blocks.HAY_BLOCK));
	}

	@Override
	//? if >=1.21.2 {
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
		BlockState neighbour, RandomSource random) {
	//?} else {
	/*public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
	*///?}
		return direction == Direction.DOWN ? state.setValue(SIGNAL_FIRE, neighbour.is(Blocks.HAY_BLOCK)) : state;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new Flue(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() && type == Hollowmere.CHIMNEY ? (tickLevel, pos, tickState, flue) -> smoke(tickLevel, pos, tickState) : null;
	}

	// 0.11 chance of 2 to 3 puffs per tick, a campfire's smoke rate
	private static void smoke(Level level, BlockPos pos, BlockState state) {
		RandomSource random = level.getRandom();
		if (random.nextFloat() < 0.11F) {
			for (int i = random.nextInt(2) + 2; i > 0; i--) {
				CampfireBlock.makeParticles(level, pos, state.getValue(SIGNAL_FIRE), false);
			}
		}
	}

	public static class Flue extends BlockEntity {
		public Flue(BlockPos pos, BlockState state) {
			super(Hollowmere.CHIMNEY, pos, state);
		}
	}
}

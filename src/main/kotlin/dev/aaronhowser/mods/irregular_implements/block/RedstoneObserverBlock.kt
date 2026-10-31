package dev.aaronhowser.mods.irregular_implements.block

import com.mojang.serialization.MapCodec
import dev.aaronhowser.mods.irregular_implements.block_entity.RedstoneObserverBlockEntity
import dev.aaronhowser.mods.irregular_implements.registry.ModBlockEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty

//TODO: Needs new name
class RedstoneObserverBlock : Block(Properties.ofFullCopy(Blocks.IRON_BLOCK)), EntityBlock {

	companion object {
		@JvmField
		val CODEC: MapCodec<RedstoneObserverBlock> = simpleCodec { RedstoneObserverBlock() }

		@JvmField
		val POWER: IntegerProperty = BlockStateProperties.POWER

		@JvmField
		val DIRECT: IntegerProperty = IntegerProperty.create("direct",0,15)

		@JvmField
		val ANALOG: IntegerProperty = IntegerProperty.create("analog",0,15)
	}

	public override fun codec(): MapCodec<RedstoneObserverBlock> {
		return CODEC
	}

	init {
		registerDefaultState(
			stateDefinition.any()
				.setValue(POWER, 0)
				.setValue(DIRECT, 0)
				.setValue(ANALOG, 0)
		)
	}

	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
		return RedstoneObserverBlockEntity(pos, state)
	}

	override fun <T : BlockEntity?> getTicker(
		level: Level,
		state: BlockState,
		blockEntityType: BlockEntityType<T>
	): BlockEntityTicker<T>? {

		return BaseEntityBlock.createTickerHelper(
			blockEntityType,
			ModBlockEntityTypes.REDSTONE_OBSERVER.get(),
			RedstoneObserverBlockEntity::tick
		)
	}

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
		builder.add(POWER, DIRECT, ANALOG)
	}

	override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
		return defaultBlockState()
			.setValue(POWER, 0)
			.setValue(DIRECT, 0)
			.setValue(ANALOG, 0)
	}

	override fun onPlace(
		state: BlockState,
		level: Level,
		pos: BlockPos,
		oldState: BlockState,
		isMoving: Boolean
	) {
		super.onPlace(state, level, pos, oldState, isMoving)

		if (level.isClientSide || isMoving) return

		val newPlaced = oldState.block !== this

		val powerChanged = newPlaced ||
				state.getValue(POWER) != oldState.getValue(POWER) ||
				state.getValue(DIRECT) != oldState.getValue(DIRECT)
		val analogChanged = newPlaced ||
				state.getValue(ANALOG) != oldState.getValue(ANALOG)

		updateNeighborPower(level, pos, powerChanged, analogChanged)

	}


	override fun onRemove(
		state: BlockState,
		level: Level,
		pos: BlockPos,
		newState: BlockState,
		isMoving: Boolean
	) {
		super.onRemove(state, level, pos, newState, isMoving)

		if (!level.isClientSide && !isMoving && state.block !== newState.block) {
			updateNeighborPower(level, pos, power = true, analog = true)
		}
	}

	private fun updateNeighborPower(level: Level, pos: BlockPos, power: Boolean, analog: Boolean) {
		if (power) {
			for (direction in Direction.entries) {
				val neighborPos = pos.relative(direction)
				level.neighborChanged(neighborPos, this, pos)
				level.updateNeighborsAtExceptFromFacing(neighborPos, this, direction.opposite)
			}
		}

		if (analog) {
			level.updateNeighbourForOutputSignal(pos, this)
		}
	}

	override fun hasAnalogOutputSignal(state: BlockState): Boolean {
		return true
	}

	override fun isSignalSource(state: BlockState): Boolean {
		return true
	}

	override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
		return state.getValue(POWER)
	}

	override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
		return state.getValue(DIRECT)
	}

	override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int {
		return state.getValue(ANALOG)
	}

}
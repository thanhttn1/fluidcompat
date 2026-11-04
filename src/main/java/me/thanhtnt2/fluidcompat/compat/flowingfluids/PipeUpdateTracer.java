package me.thanhtnt2.fluidcompat.compat.flowingfluids;

import java.util.Set;

import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Debug mode only: logs every block update of a pipe together with its fluid, so a pipe that flickers
 * between submerged and dry shows up as a clear sequence in logs/fluidcompat.log.
 */
final class PipeUpdateTracer {

    private final Set<Block> pipes;

    private PipeUpdateTracer(Set<Block> pipes) {
        this.pipes = pipes;
    }

    static void register(Set<Block> pipes) {
        NeoForge.EVENT_BUS.addListener(new PipeUpdateTracer(pipes)::onNeighborNotify);
    }

    private void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        BlockState state = event.getState();
        if (event.getLevel().isClientSide() || !pipes.contains(state.getBlock())) {
            return;
        }
        boolean waterlogged = state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED);
        FluidCompatLog.trace("Pipe update block={} pos={} waterlogged={} stateFluid={}/{} worldFluid={}",
                BuiltInRegistries.BLOCK.getKey(state.getBlock()), event.getPos().toShortString(), waterlogged,
                BuiltInRegistries.FLUID.getKey(state.getFluidState().getType()), state.getFluidState().getAmount(),
                BuiltInRegistries.FLUID.getKey(event.getLevel().getFluidState(event.getPos()).getType()));
    }
}

package me.thanhtnt2.fluidcompat.compat.pneumaticcraft;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import me.thanhtnt2.fluidcompat.api.FiniteFluidService;
import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.core.balance.AutoBalance;
import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import me.thanhtnt2.fluidcompat.api.FluidIntakeRequest;
import me.thanhtnt2.fluidcompat.api.FluidIntakeResult;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import me.thanhtnt2.fluidcompat.logging.FluidCompatLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Lets a Liquid Hopper drain the thin leftover water of a pond into any tank, through the public NeoForge fluid
 * capability and the Fluid Compat intake API. PneumaticCraft itself only picks up whole source blocks, so what
 * is left when a pond runs low would otherwise stay behind.
 *
 * <p>It never links against PneumaticCraft classes: the two direction getters are called by name.
 * Every failure falls back to "do nothing", so the hopper keeps working exactly as PneumaticCraft made it.
 */
public final class PneumaticHopperPuller {

    // Looked up once per hopper class, so an idle hopper allocates nothing and builds no strings on every call
    private static final ClassValue<Method> INPUT_GETTER = new ClassValue<>() {
        @Override
        protected Method computeValue(Class<?> type) {
            return findPublicGetter(type, "getInputDirection");
        }
    };
    private static final ClassValue<Method> OUTPUT_GETTER = new ClassValue<>() {
        @Override
        protected Method computeValue(Class<?> type) {
            return findPublicGetter(type, "getRotation");
        }
    };

    // "Look again at game time X" for idle hoppers. Weak keys: a hopper that unloads is simply forgotten.
    // Deliberately not saved with the block entity, so an idle hopper never marks its chunk as changed.
    private static final Map<BlockEntity, Long> IDLE_UNTIL = Collections.synchronizedMap(new WeakHashMap<>());

    private PneumaticHopperPuller() {
    }

    /**
     * @param rateUnits the transfer-rate argument PneumaticCraft passes to {@code doImport} (100 mB per unit)
     * @return true when water was moved into the hopper this call
     */
    public static boolean tryPullLeftover(BlockEntity hopper, int rateUnits) {
        if (!(hopper.getLevel() instanceof ServerLevel level) || !FluidCompatApi.isReady()) {
            return false;
        }
        try {
            return pull(level, hopper, rateUnits);
        } catch (RuntimeException | LinkageError error) {
            FluidCompatLog.errorThrottled("pneumaticcraft-hopper-pull",
                    "Liquid Hopper leftover water pull failed at " + hopper.getBlockPos().toShortString(), error);
            return false;
        }
    }

    private static boolean pull(ServerLevel level, BlockEntity hopper, int rateUnits) {
        if (!CompatGate.masterEnabled() || !FluidCompatModulesConfig.PNC_LEFTOVER_WATER.get()) {
            return false;
        }
        HopperPullSettings settings = settingsOf(hopper);
        long now = level.getGameTime();
        Long idleUntil = IDLE_UNTIL.get(hopper);
        if (!settings.enabled() || now < settings.nextAttemptGameTime() || (idleUntil != null && now < idleUntil)) {
            return false;
        }
        Direction input = directionOf(INPUT_GETTER.get(hopper.getClass()), hopper);
        Direction output = directionOf(OUTPUT_GETTER.get(hopper.getClass()), hopper);
        if (input == null || output == null) {
            return false;
        }
        BlockPos hopperPos = hopper.getBlockPos();
        BlockPos inputPos = hopperPos.relative(input);
        int leftoverLevels = leftoverLevelsAt(level, inputPos);
        boolean hasUpgrade = PneumaticPickupBridge.pickupWasAttempted(level, inputPos);
        if (leftoverLevels <= 0) {
            // The common case for a hopper: nothing to drain here. Check again in a moment, not on every call.
            idleFor(level, hopper);
            return false;
        }
        if (!(hasUpgrade || FluidCompatModulesConfig.PNC_LEFTOVER_WITHOUT_UPGRADE.get())) {
            return skip(hopper, "no Dispenser Upgrade and liquidHopperLeftoverWithoutUpgrade is off");
        }
        FiniteFluidService service = FluidCompatApi.service();
        if (!service.isFinite(Fluids.WATER)) {
            return false;
        }
        IFluidHandler hopperTank = level.getCapability(Capabilities.FluidHandler.BLOCK, hopperPos, input);
        if (hopperTank == null) {
            return skip(hopper, "the hopper exposes no tank on its input side");
        }
        int levels = levelsTheTankAccepts(hopperTank, rateUnits);
        if (!hasUpgrade) {
            // Without the upgrade the hopper only gets the thin leftover, never more than the input cell holds
            levels = Math.min(levels, leftoverLevels);
        }
        if (levels <= 0) {
            return skip(hopper, "the hopper tank is full");
        }
        if (!hasConnectedOutput(level, hopperPos, output)) {
            return skip(hopper, "liquidHopperLeftoverNeedsOutput is on and nothing on the output side accepts water");
        }
        return drainInto(level, hopper, settings, hopperTank, inputPos, levels);
    }

    // Says why a hopper that found leftover water did nothing; written only while verbose/debug logging is on
    private static boolean skip(BlockEntity hopper, String reason) {
        if (FluidCompatLog.isDetailLoggingActive()) {
            FluidCompatLog.debugThrottled("pnc-skip-" + hopper.getBlockPos().asLong(),
                    "[pneumaticcraft] Liquid Hopper at {} skipped the leftover pull: {}",
                    hopper.getBlockPos().toShortString(), reason);
        }
        return false;
    }

    // getData would attach (and later save) a default value on every hopper that merely runs this check,
    // so a hopper nobody ever configured stays free of Fluid Compat data
    private static HopperPullSettings settingsOf(BlockEntity hopper) {
        AttachmentType<HopperPullSettings> type = PneumaticAttachments.HOPPER_PULL_SETTINGS.get();
        return hopper.hasData(type) ? hopper.getData(type) : HopperPullSettings.DEFAULT;
    }

    private static void idleFor(ServerLevel level, BlockEntity hopper) {
        int waitTicks = AutoBalance.scaleTicks(level.getServer(), FluidCompatModulesConfig.PNC_EMPTY_RETRY_TICKS.get());
        IDLE_UNTIL.put(hopper, level.getGameTime() + waitTicks);
    }

    private static boolean drainInto(ServerLevel level, BlockEntity hopper, HopperPullSettings settings,
            IFluidHandler hopperTank, BlockPos inputPos, int levels) {
        ResourceLocation machineId = BuiltInRegistries.BLOCK.getKey(hopper.getBlockState().getBlock());
        FluidIntakeRequest request = new FluidIntakeRequest(
                level, hopper.getBlockPos(), machineId, Fluids.WATER, levels, List.of(inputPos));
        FluidIntakeResult result = FluidCompatApi.service().requestIntake(request);
        if (!result.managed()) {
            return false;
        }
        if (result.drainedLevels() <= 0) {
            // Nothing left to take: stay quiet for a while instead of asking every tick
            idleFor(level, hopper);
            return false;
        }
        int millibuckets = result.drainedLevels() * FluidCompatApi.MILLIBUCKETS_PER_LEVEL;
        int filled = hopperTank.fill(new FluidStack(Fluids.WATER, millibuckets), IFluidHandler.FluidAction.EXECUTE);
        FluidCompatLog.debugThrottled("pnc-hopper@" + hopper.getBlockPos().asLong(),
                "Liquid Hopper leftover pull pos={} drainedLevels={} filledMb={}",
                hopper.getBlockPos().toShortString(), result.drainedLevels(), filled);
        return filled > 0;
    }

    /**
     * The partial water amount (1 to 7 levels) of a real fluid cell, the "leftover" PneumaticCraft cannot pick up.
     * Waterlogged blocks and full source blocks give 0: those are not leftover.
     */
    private static int leftoverLevelsAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LiquidBlock)) {
            return 0;
        }
        FluidState fluidState = state.getFluidState();
        if (!fluidState.is(FluidTags.WATER) || fluidState.isSource()) {
            return 0;
        }
        return fluidState.getAmount();
    }

    // At least one whole level per call, otherwise the 100 mB rate unit would never add up to a level
    private static int levelsTheTankAccepts(IFluidHandler hopperTank, int rateUnits) {
        int wantedLevels = Math.max(1, Math.ceilDiv(Math.max(1, rateUnits) * 100, FluidCompatApi.MILLIBUCKETS_PER_LEVEL));
        int accepted = hopperTank.fill(
                new FluidStack(Fluids.WATER, wantedLevels * FluidCompatApi.MILLIBUCKETS_PER_LEVEL), IFluidHandler.FluidAction.SIMULATE);
        return accepted / FluidCompatApi.MILLIBUCKETS_PER_LEVEL;
    }

    private static boolean hasConnectedOutput(ServerLevel level, BlockPos hopperPos, Direction output) {
        // Only the new key counts: the old requireOutputContainer defaulted to true and silently blocked hoppers without an output
        if (!FluidCompatModulesConfig.PNC_LEFTOVER_NEEDS_OUTPUT.get()) {
            return true;
        }
        IFluidHandler container = level.getCapability(
                Capabilities.FluidHandler.BLOCK, hopperPos.relative(output), output.getOpposite());
        return container != null
                && container.fill(new FluidStack(Fluids.WATER, FluidCompatApi.MILLIBUCKETS_PER_LEVEL), IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    private static Direction directionOf(Method getter, BlockEntity hopper) {
        try {
            return getter == null ? null : (Direction) getter.invoke(hopper);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return null;
        }
    }

    private static Method findPublicGetter(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException missing) {
            return null;
        }
    }
}

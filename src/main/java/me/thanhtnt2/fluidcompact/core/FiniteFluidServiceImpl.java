package me.thanhtnt2.fluidcompact.core;

import java.util.Optional;

import me.thanhtnt2.fluidcompact.api.DrainBackend;
import me.thanhtnt2.fluidcompact.api.DrainBackendRegistry;
import me.thanhtnt2.fluidcompact.api.FiniteFluidService;
import me.thanhtnt2.fluidcompact.api.FluidCompactTags;
import me.thanhtnt2.fluidcompact.api.FluidIntakeRequest;
import me.thanhtnt2.fluidcompact.api.FluidIntakeResult;
import me.thanhtnt2.fluidcompact.api.event.FluidIntakeEvent;
import me.thanhtnt2.fluidcompact.config.BackendPreference;
import me.thanhtnt2.fluidcompact.config.FluidCompactConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.NeoForge;

/** Default {@link FiniteFluidService}: applies the config, fires events and delegates to a backend. */
public final class FiniteFluidServiceImpl implements FiniteFluidService {

    @Override
    public boolean isFinite(Fluid fluid) {
        if (!FluidCompactConfig.ENABLE_FINITE_FLUIDS.get()) {
            return false;
        }
        if (fluid.is(FluidTags.WATER)) {
            return FluidCompactConfig.FINITE_WATER.get();
        }
        if (fluid.is(FluidTags.LAVA)) {
            return FluidCompactConfig.FINITE_LAVA.get();
        }
        return fluid.is(FluidCompactTags.MANAGED_FLUIDS) && FluidCompactConfig.FINITE_TAGGED_FLUIDS.get();
    }

    @Override
    public FluidIntakeResult requestIntake(FluidIntakeRequest request) {
        if (!isFinite(request.fluid())) {
            return FluidIntakeResult.unmanaged(request.requestedLevels());
        }

        FluidIntakeEvent.Pre preEvent = new FluidIntakeEvent.Pre(request);
        NeoForge.EVENT_BUS.post(preEvent);
        if (preEvent.isCanceled()) {
            return FluidIntakeResult.unmanaged(request.requestedLevels());
        }

        Optional<DrainBackend> backend = activeBackendFor(request.level(), request.fluid());
        if (backend.isEmpty()) {
            // Nobody can drain this fluid, so keeping the machine working beats silently breaking it
            return FluidIntakeResult.unmanaged(request.requestedLevels());
        }

        int levelsToDrain = preEvent.getRequestedLevels();
        int drainedLevels = drainAcrossSources(backend.get(), request, levelsToDrain);
        FluidIntakeResult result = new FluidIntakeResult(levelsToDrain, drainedLevels, true);
        NeoForge.EVENT_BUS.post(new FluidIntakeEvent.Post(request, result));
        return result;
    }

    @Override
    public Optional<DrainBackend> activeBackendFor(ServerLevel level, Fluid fluid) {
        BackendPreference preference = FluidCompactConfig.BACKEND_PREFERENCE.get();
        // A plain loop: this runs for every intake, and a stream with two lambdas allocates on each call
        for (DrainBackend backend : DrainBackendRegistry.snapshot()) {
            if (preference.accepts(backend.id()) && backend.supports(level, fluid)) {
                return Optional.of(backend);
            }
        }
        return Optional.empty();
    }

    private int drainAcrossSources(DrainBackend backend, FluidIntakeRequest request, int levelsToDrain) {
        int remaining = levelsToDrain;
        for (BlockPos sourcePos : request.sourcePositions()) {
            if (remaining <= 0) {
                break;
            }
            int removed = backend.drain(request.level(), sourcePos, request.fluid(), remaining);
            // Clamp so a misbehaving addon backend can never create fluid out of thin air
            remaining -= Math.max(0, Math.min(removed, remaining));
        }
        return levelsToDrain - remaining;
    }
}

package me.thanhtnt2.fluidcompat.compat.pneumaticcraft.network;

import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.HopperPullSettings;
import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.PneumaticAttachments;
import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.PneumaticHopperTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The two small packets behind the Liquid Hopper button. Optional on the client, so servers can run without it installed there. */
public final class PneumaticNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static final double MAX_INTERACTION_DISTANCE = 4.0D;

    private PneumaticNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        registrar.playToServer(HopperPullRequest.TYPE, HopperPullRequest.STREAM_CODEC, PneumaticNetwork::handleRequest);
        registrar.playToClient(HopperPullState.TYPE, HopperPullState.STREAM_CODEC, PneumaticNetwork::handleState);
    }

    private static void handleRequest(HopperPullRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                answer(player, request);
            }
        });
    }

    private static void answer(ServerPlayer player, HopperPullRequest request) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = request.pos();
        // Never trust a position sent by a client: it must be loaded, close to the player and really a Liquid Hopper
        if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, MAX_INTERACTION_DISTANCE)) {
            return;
        }
        BlockEntity hopper = level.getBlockEntity(pos);
        if (!PneumaticHopperTargeting.isLiquidHopper(hopper)) {
            return;
        }
        HopperPullSettings settings = hopper.getData(PneumaticAttachments.HOPPER_PULL_SETTINGS.get());
        if (request.toggle()) {
            settings = settings.withEnabled(!settings.enabled());
            hopper.setData(PneumaticAttachments.HOPPER_PULL_SETTINGS.get(), settings);
            hopper.setChanged();
        }
        PacketDistributor.sendToPlayer(player, new HopperPullState(pos, settings.enabled()));
    }

    private static void handleState(HopperPullState state, IPayloadContext context) {
        context.enqueueWork(() -> PneumaticClientState.update(state.pos(), state.enabled()));
    }
}

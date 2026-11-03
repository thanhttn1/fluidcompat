package me.thanhtnt2.fluidcompat.compat.pneumaticcraft.network;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: ask for a Liquid Hopper's switch state, optionally flipping it first. */
public record HopperPullRequest(BlockPos pos, boolean toggle) implements CustomPacketPayload {

    public static final Type<HopperPullRequest> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FluidCompatApi.MOD_ID, "hopper_pull_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HopperPullRequest> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HopperPullRequest::pos,
            ByteBufCodecs.BOOL, HopperPullRequest::toggle,
            HopperPullRequest::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

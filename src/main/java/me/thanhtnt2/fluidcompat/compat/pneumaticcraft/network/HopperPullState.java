package me.thanhtnt2.fluidcompat.compat.pneumaticcraft.network;

import me.thanhtnt2.fluidcompat.api.FluidCompatApi;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the current switch state of a Liquid Hopper. */
public record HopperPullState(BlockPos pos, boolean enabled) implements CustomPacketPayload {

    public static final Type<HopperPullState> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FluidCompatApi.MOD_ID, "hopper_pull_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HopperPullState> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HopperPullState::pos,
            ByteBufCodecs.BOOL, HopperPullState::enabled,
            HopperPullState::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

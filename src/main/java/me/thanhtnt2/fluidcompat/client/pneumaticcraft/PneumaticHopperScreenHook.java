package me.thanhtnt2.fluidcompat.client.pneumaticcraft;

import me.thanhtnt2.fluidcompat.compat.common.CompatGate;
import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.PneumaticHopperTargeting;
import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.network.HopperPullRequest;
import me.thanhtnt2.fluidcompat.config.FluidCompatModulesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Adds the switch to PneumaticCraft's Liquid Hopper screen through NeoForge's screen event: no mixin and no
 * PneumaticCraft class involved, the screen is recognised by its class name. Client only.
 */
public final class PneumaticHopperScreenHook {

    private static final String LIQUID_HOPPER_SCREEN = "me.desht.pneumaticcraft.client.gui.LiquidHopperScreen";

    // Free space between the upgrade slots (left) and the tank (right) of the hopper screen
    private static final int BUTTON_X = 88;
    private static final int BUTTON_Y = 20;

    private PneumaticHopperScreenHook() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PneumaticHopperScreenHook::onScreenInit);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!LIQUID_HOPPER_SCREEN.equals(screen.getClass().getName())
                || !(screen instanceof AbstractContainerScreen<?> container)
                || !CompatGate.masterEnabled()
                || !FluidCompatModulesConfig.PNC_GUI_BUTTON.get()) {
            return;
        }
        BlockPos hopperPos = lookedAtHopper();
        if (hopperPos == null) {
            return;
        }
        event.addListener(new HopperPullButton(container.getGuiLeft() + BUTTON_X, container.getGuiTop() + BUTTON_Y, hopperPos));
        // Ask the server for the current state so the button shows the right icon straight away
        PacketDistributor.sendToServer(new HopperPullRequest(hopperPos, false));
    }

    // The screen was opened by right-clicking the hopper, so the block under the crosshair is the hopper
    private static BlockPos lookedAtHopper() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)) {
            return null;
        }
        BlockEntity blockEntity = minecraft.level.getBlockEntity(hit.getBlockPos());
        return PneumaticHopperTargeting.isLiquidHopper(blockEntity) ? hit.getBlockPos() : null;
    }
}

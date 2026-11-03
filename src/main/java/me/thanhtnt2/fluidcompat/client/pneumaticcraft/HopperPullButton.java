package me.thanhtnt2.fluidcompat.client.pneumaticcraft;

import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.network.HopperPullRequest;
import me.thanhtnt2.fluidcompat.compat.pneumaticcraft.network.PneumaticClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Fluid Compat switch inside the Liquid Hopper screen.
 * It copies the look of PneumaticCraft's own mode buttons: a plain 20x20 vanilla button with an item as its icon.
 */
final class HopperPullButton extends Button {

    private static final int SIZE = 20;
    private static final int ICON_INSET = 2;
    private static final int DISABLED_SHADE = 0x99000000;

    private final BlockPos hopperPos;
    private final ItemStack icon = new ItemStack(Items.WATER_BUCKET);
    private Boolean shownState;

    HopperPullButton(int x, int y, BlockPos hopperPos) {
        super(x, y, SIZE, SIZE, Component.empty(),
                button -> PacketDistributor.sendToServer(new HopperPullRequest(hopperPos, true)),
                DEFAULT_NARRATION);
        this.hopperPos = hopperPos;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        boolean enabled = PneumaticClientState.isEnabled(hopperPos);
        refreshTooltip(enabled);
        graphics.renderItem(icon, getX() + ICON_INSET, getY() + ICON_INSET);
        if (!enabled) {
            graphics.fill(getX() + 1, getY() + 1, getX() + SIZE - 1, getY() + SIZE - 1, DISABLED_SHADE);
            Component cross = Component.literal("X").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            graphics.drawCenteredString(Minecraft.getInstance().font, cross, getX() + SIZE / 2, getY() + 6, 0xFFFFFF);
        }
    }

    // The tooltip object is only rebuilt when the state flips, not every frame
    private void refreshTooltip(boolean enabled) {
        if (shownState != null && shownState == enabled) {
            return;
        }
        shownState = enabled;
        String key = enabled ? "fluidcompact.pnc.hopper_pull.on" : "fluidcompact.pnc.hopper_pull.off";
        setTooltip(Tooltip.create(Component.translatable(key)));
    }
}

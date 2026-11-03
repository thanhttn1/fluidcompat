package me.thanhtnt2.fluidcompat.compat.pneumaticcraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-hopper settings, stored as a data attachment on the Liquid Hopper's block entity.
 * Two flat values only (no positions or lists), so it can never grow.
 *
 * @param enabled               whether the player left the Fluid Compat water pull switched on for this hopper
 * @param nextAttemptGameTime   game time before which the hopper does not look for leftover water again
 */
// nextAttemptGameTime is no longer written by anything (the idle delay lives in memory now). The component stays so
// worlds saved by 1.2.x and 1.3.x keep loading; a default value is not written to the save at all.
public record HopperPullSettings(boolean enabled, long nextAttemptGameTime) {

    public static final HopperPullSettings DEFAULT = new HopperPullSettings(true, 0L);

    public static final Codec<HopperPullSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(HopperPullSettings::enabled),
            Codec.LONG.optionalFieldOf("next_attempt_game_time", 0L).forGetter(HopperPullSettings::nextAttemptGameTime)
    ).apply(instance, HopperPullSettings::new));

    public HopperPullSettings withEnabled(boolean newEnabled) {
        return new HopperPullSettings(newEnabled, nextAttemptGameTime);
    }
}

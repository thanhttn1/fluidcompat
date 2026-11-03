package me.thanhtnt2.fluidcompat.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Lifetime numbers of one pump, stored as a data attachment on its block entity.
 *
 * <p>Deliberately four flat numbers and nothing else: no block positions, lists or references, so the data
 * of a pump can never grow with the size of the world or the time it has been running.
 *
 * @param lastIntakeGameTime game time of the last drain, or -1 when the pump never drained anything
 */
public record PumpStatistics(
        long totalDrainedLevels,
        long completedIntakes,
        long starvedIntakes,
        long lastIntakeGameTime) {

    public static final PumpStatistics EMPTY = new PumpStatistics(0L, 0L, 0L, -1L);

    // optionalFieldOf keeps old saves loadable when new fields are added later
    public static final Codec<PumpStatistics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("total_drained_levels", 0L).forGetter(PumpStatistics::totalDrainedLevels),
            Codec.LONG.optionalFieldOf("completed_intakes", 0L).forGetter(PumpStatistics::completedIntakes),
            Codec.LONG.optionalFieldOf("starved_intakes", 0L).forGetter(PumpStatistics::starvedIntakes),
            Codec.LONG.optionalFieldOf("last_intake_game_time", -1L).forGetter(PumpStatistics::lastIntakeGameTime)
    ).apply(instance, PumpStatistics::new));

    public PumpStatistics withIntake(int drainedLevels, boolean fullyServed, long gameTime) {
        long starved = fullyServed ? starvedIntakes : starvedIntakes + 1L;
        return new PumpStatistics(totalDrainedLevels + drainedLevels, completedIntakes + 1L, starved, gameTime);
    }

    public boolean isCoolingDown(long gameTime, int minTicksBetweenIntakes) {
        if (minTicksBetweenIntakes <= 0 || lastIntakeGameTime < 0L) {
            return false;
        }
        return gameTime - lastIntakeGameTime < minTicksBetweenIntakes;
    }
}

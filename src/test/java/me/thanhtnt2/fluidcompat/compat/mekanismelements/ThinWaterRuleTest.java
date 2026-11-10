package me.thanhtnt2.fluidcompat.compat.mekanismelements;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit level only: the rule behind the Seawater Pump thin water hook. Says nothing about the running game. */
class ThinWaterRuleTest {

    @Test
    void everyFlowingLevelCountsForThePump() {
        for (int levels = 1; levels <= 7; levels++) {
            assertTrue(ThinWaterRule.isUsableLayer(levels), "level " + levels);
        }
    }

    @Test
    void anEmptyCellNeverCounts() {
        assertFalse(ThinWaterRule.isUsableLayer(0));
        assertFalse(ThinWaterRule.isUsableLayer(-1));
    }
}

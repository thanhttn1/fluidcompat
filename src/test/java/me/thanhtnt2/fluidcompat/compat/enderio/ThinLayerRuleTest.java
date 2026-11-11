package me.thanhtnt2.fluidcompat.compat.enderio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit level only: the number rules of the Ender IO hooks. Says nothing about the running game. */
class ThinLayerRuleTest {

    @Test
    void everyFlowingLevelIsUsableAndEmptyIsNot() {
        for (int levels = 1; levels <= 7; levels++) {
            assertTrue(ThinLayerRule.isUsableLayer(levels));
        }
        assertFalse(ThinLayerRule.isUsableLayer(0));
    }

    @Test
    void niardIsChargedExactlyTheGainOverALayer() {
        // The world gains (8 - held) levels of 125 mB; the Niard pays exactly that, so nothing is lost
        for (int held = 0; held <= 7; held++) {
            assertEquals((8 - held) * 125, ThinLayerRule.niardChargeMillibuckets(held), "held " + held);
        }
    }

    @Test
    void niardChargeStaysInsideOneBucketAndNeverBelowOneLevel() {
        assertEquals(1000, ThinLayerRule.niardChargeMillibuckets(-3));
        assertEquals(125, ThinLayerRule.niardChargeMillibuckets(8));
        assertEquals(125, ThinLayerRule.niardChargeMillibuckets(100));
    }
}

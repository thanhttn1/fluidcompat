package me.thanhtnt2.fluidcompat.compat.flowingfluids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit level only: the per-tick limits of the orphan pipe cleanup. */
class CleanupBudgetTest {

    private static int chunksWorkedOff(CleanupBudget budget, int blockEntitiesPerChunk, int queued) {
        int taken = 0;
        while (taken < queued && budget.mayTakeChunk()) {
            budget.tookChunk(blockEntitiesPerChunk);
            taken++;
        }
        return taken;
    }

    @Test
    void emptyChunksAreLimitedByTheChunkCount() {
        assertEquals(8, chunksWorkedOff(new CleanupBudget(8, 2048, 1), 0, 81));
    }

    @Test
    void denseChunksAreLimitedByBlockEntities() {
        // 2048 / 1500 -> the second chunk uses up the budget, the third is not started
        CleanupBudget budget = new CleanupBudget(8, 2048, 1);
        assertEquals(2, chunksWorkedOff(budget, 1500, 81));
        assertEquals(3000, budget.blockEntitiesVisited());
    }

    @Test
    void aSingleHugeChunkIsStillProcessedSoTheQueueMakesProgress() {
        CleanupBudget budget = new CleanupBudget(8, 2048, 1);
        assertTrue(budget.mayTakeChunk());
        budget.tookChunk(50_000);
        assertFalse(budget.mayTakeChunk());
        assertEquals(1, budget.chunksTaken());
    }

    @Test
    void slowdownShrinksBothLimitsButNeverToZero() {
        assertEquals(2, chunksWorkedOff(new CleanupBudget(8, 2048, 4), 0, 81));
        assertEquals(1, chunksWorkedOff(new CleanupBudget(8, 2048, 1000), 0, 81));
        assertEquals(1, chunksWorkedOff(new CleanupBudget(8, 2048, 0), 4096, 81));
    }

    @Test
    void anEmptyQueueTakesNothing() {
        assertEquals(0, chunksWorkedOff(new CleanupBudget(8, 2048, 1), 10, 0));
    }
}

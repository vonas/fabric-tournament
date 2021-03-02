package io.github.vonas.tournament.core;

import io.github.vonas.tournament.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RoundTest {

    private UUID[] UUIDs = {
        UUID.fromString("e229d301-2a54-421a-8e26-8aff8e3dc340"),
        UUID.fromString("25624446-a0dd-4781-88d9-2072c92c79fe"),
        UUID.fromString("c208ed38-cdfe-4557-8cca-7c5191d7278f")
    };

    private DynamicRound round;
    private DynamicRound twoEntrantRound;

    private final Entrant entrantA = new Entrant(UUIDs[0]);
    private final Entrant entrantB = new Entrant(UUIDs[1]);
    private final Entrant invalidEntrant = new Entrant(UUIDs[2]);

    private final Entrant winner = entrantA;
    private final Entrant loser = entrantB;

    @BeforeEach
    void init() {
        round = new DynamicRound();
        twoEntrantRound = new DynamicRound(Arrays.asList(entrantA, entrantB));
    }

    // TODO Add test that checks if nextPairing()
    //  shuffles the remaining pending entrants.

    @Test
    void noEntrants_addEntrant_isPending() {
        round.addEntrant(entrantA);
        List<Entrant> pendingEntrants = round.getPendingEntrants();
        assertEquals(1, pendingEntrants.size());
        assertEquals(entrantA, pendingEntrants.get(0));
    }

    @Test
    void noEntrants_nextPairing_throwsNoEntrants() {
        assertThrows(NoEntrantsException.class, () -> round.nextPairing());
    }

    @Test
    void oneEntrant_nextPairing_throwsNoOpponent() {
        round.addEntrant(entrantA);
        assertThrows(NoOpponentException.class, () -> round.nextPairing());
    }

    @Test
    void twoEntrants_nextPairing_noPendingEntrants() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        assertTrue(twoEntrantRound.getPendingEntrants().isEmpty());
    }

    @Test
    void twoEntrants_nextPairing_returnsBothEntrants() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        Entrant first = pairing.getEntrant1();
        Entrant second = pairing.getEntrant2();
        if (first != entrantA) {
            Entrant tmp = first;
            first = second;
            second = tmp;
        }
        assertEquals(entrantA, first);
        assertEquals(entrantB, second);
    }

    @Test
    void twoEntrants_nextPairing_updatesCurrentPairing() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        assertTrue(twoEntrantRound.hasActivePairing());
        assertEquals(pairing, twoEntrantRound.getActivePairing());
    }

    @Test
    void pairing_declareInvalidWinner_throwsNoSuchEntrant() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        assertThrows(NoSuchEntrantException.class, () -> twoEntrantRound.declareWinner(invalidEntrant));
    }

    @Test
    void noPairing_declareWinner_throwsMissingPairing() {
        assertThrows(MissingPairingException.class, () -> twoEntrantRound.declareWinner(entrantA));
    }

    @Test
    void pairing_declareWinner_winnerAdvancesAndLoserEliminated() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        twoEntrantRound.declareWinner(winner);
        Set<Entrant> advancedEntrants = twoEntrantRound.getAdvancedEntrants();
        Set<Entrant> eliminatedEntrants = twoEntrantRound.getEliminatedEntrants();
        assertEquals(1, advancedEntrants.size());
        assertEquals(1, eliminatedEntrants.size());
        assertTrue(advancedEntrants.contains(winner));
        assertTrue(eliminatedEntrants.contains(loser));
    }

    @Test
    void pairing_declareWinner_pairingFinished() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        twoEntrantRound.declareWinner(winner);
        Set<Pairing> finishedPairings = twoEntrantRound.getFinishedPairings();
        assertTrue(finishedPairings.contains(pairing));
        assertEquals(1, finishedPairings.size());
    }

    @Test
    void twoEntrants_declareWinner_roundFinished() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        twoEntrantRound.declareWinner(winner);
        assertTrue(twoEntrantRound.isFinished());
    }

    @Test
    void eliminatedEntrant_reviveEliminated_eliminatedPending() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        twoEntrantRound.declareWinner(winner);
        twoEntrantRound.reviveEntrant(loser);
        assertTrue(twoEntrantRound.getEliminatedEntrants().isEmpty());
        assertTrue(twoEntrantRound.getPendingEntrants().contains(loser));
    }

    @Test
    void oneEntrant_removeEntrant_noEntrants() {
        round.addEntrant(entrantA);
        round.removeEntrant(entrantA);
        assertTrue(round.getEntrants().isEmpty());
    }

    @Test
    void pairingPending_removeOneEntrant_otherEntrantPending() throws Exception {
        Pairing pendingPairing = twoEntrantRound.nextPairing();
        twoEntrantRound.removeEntrant(entrantA);
        assertTrue(twoEntrantRound.getPendingEntrants().contains(entrantB));
    }

    @Test
    void finishedRound_removeAllEntrants_noEntrantsAndPairings() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        twoEntrantRound.declareWinner(entrantA);
        twoEntrantRound.removeEntrant(entrantA);
        twoEntrantRound.removeEntrant(entrantB);
        assertEquals(0, twoEntrantRound.getEntrants().size());
        assertEquals(0, twoEntrantRound.getPendingEntrants().size());
        assertEquals(0, twoEntrantRound.getAdvancedEntrants().size());
        assertEquals(0, twoEntrantRound.getEliminatedEntrants().size());
        assertEquals(0, twoEntrantRound.getFinishedPairings().size());
    }

    @Test
    void finishedRound_removeLoser_keepAdvancedEntrantsFinishedPairing() throws Exception {
        Pairing pairing = twoEntrantRound.nextPairing();
        twoEntrantRound.declareWinner(winner);
        twoEntrantRound.removeEntrant(loser);
        assertTrue(twoEntrantRound.getFinishedPairings().contains(pairing));
    }
}

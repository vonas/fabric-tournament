package de.j13g.manko.core.rounds;

import de.j13g.manko.core.Pairing;
import de.j13g.manko.core.Placement;
import de.j13g.manko.core.annotations.UnsupportedOperation;
import de.j13g.manko.core.base.FinalRound;
import de.j13g.manko.core.base.RankingRound;
import de.j13g.manko.core.managers.PairingManager;
import de.j13g.manko.core.exceptions.*;
import de.j13g.manko.core.managers.PlacementManager;
import de.j13g.manko.core.managers.base.Pairings;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

public class Final<E> implements RankingRound<E>, FinalRound<E>, Serializable {

    private final Pairing<E> firstPlacePairing;
    private final Pairing<E> thirdPlacePairing;

    private final Set<E> entrants = new HashSet<>();

    private final ArrayList<Pairing<E>> pairingOrder = new ArrayList<>();

    private final PairingManager<E> pairings = new PairingManager<>();
    private final PlacementManager<E> placements = new PlacementManager<>();

    public Final(Pairing<E> firstPlacePairing) {
        this(firstPlacePairing, null);
    }

    public Final(Pairing<E> firstPlacePairing, @Nullable Pairing<E> thirdPlacePairing) {

        if (thirdPlacePairing != null) {
            if (thirdPlacePairing.contains(firstPlacePairing.getFirst()))
                throw new IllegalArgumentException();
            if (thirdPlacePairing.contains(firstPlacePairing.getSecond()))
                throw new IllegalArgumentException();

            pairingOrder.add(thirdPlacePairing);
            entrants.add(thirdPlacePairing.getFirst());
            entrants.add(thirdPlacePairing.getSecond());
        }

        pairingOrder.add(firstPlacePairing);
        entrants.add(firstPlacePairing.getFirst());
        entrants.add(firstPlacePairing.getSecond());

        this.firstPlacePairing = firstPlacePairing;
        this.thirdPlacePairing = thirdPlacePairing;
    }

    @Override
    public boolean addEntrant(E entrant) throws NewEntrantsNotAllowedException {
        if (hasEntrant(entrant))
            return false;
        if (!firstPlacePairing.contains(entrant) && !thirdPlacePairing.contains(entrant))
            throw new NewEntrantsNotAllowedException();

        Pairing<E> pairing = getPairingForEntrant(entrant);

        if (!pairings.isFinished(pairing)) {
            E otherEntrant = pairing.getOther(entrant);

            placements.setPlacement(entrant, Placement.TBD);
            placements.setPlacement(otherEntrant, Placement.TBD);
        }

        if (!pairings.contains(pairing)) {
            if (pairing.equals(firstPlacePairing))
                pairingOrder.add(firstPlacePairing);
            else if (pairing.equals(thirdPlacePairing))
                pairingOrder.add(0, thirdPlacePairing);
        }

        entrants.add(entrant);

        return true;
    }

    @Override
    public boolean removeEntrant(E entrant) {
        if (!hasEntrant(entrant))
            return false;

        Pairing<E> pairing = getPairingForEntrant(entrant);

        if (!pairings.isFinished(pairing)) {
            E otherEntrant = pairing.getOther(entrant);

            placements.setPlacement(entrant, Placement.NONE);
            if (pairing.equals(firstPlacePairing))
                placements.setPlacement(otherEntrant, Placement.FIRST);
            else if (pairing.equals(thirdPlacePairing))
                placements.setPlacement(otherEntrant, Placement.THIRD);

            pairings.remove(pairing);
        }

        if (!pairings.contains(pairing))
            pairingOrder.remove(pairing);

        entrants.remove(entrant);

        return true;
    }

    @Override
    @UnsupportedOperation
    public boolean resetEntrant(E entrant) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pairing<E> nextPairing() throws UnfinishedPairingsException, NoMorePairingsException {
        if (pairingOrder.isEmpty())
            throw new NoMorePairingsException();
        if (pairings.hasActive())
            throw new UnfinishedPairingsException();

        Pairing<E> pairing = pairingOrder.remove(0);
        pairings.add(pairing);

        return pairing;
    }

    @Override
    public boolean replayPairing(Pairing<E> pairing) throws NoSuchPairingException, MissingEntrantException {
        if (pairings.isActive(pairing))
            return false;
        if (!pairings.isFinished(pairing))
            throw new NoSuchPairingException();
        if (!hasEntrant(pairing.getFirst()) || !hasEntrant(pairing.getSecond()))
            throw new MissingEntrantException();

        placements.resetPlacement(pairing.getFirst());
        placements.resetPlacement(pairing.getSecond());

        pairings.removeFinished(pairing);
        pairings.add(pairing);
        return true;
    }

    @Override
    public void declareWinner(E winningEntrant, Pairing<E> pairing)
            throws NoSuchEntrantException, NoSuchPairingException {

        if (!pairing.contains(winningEntrant))
            throw new IllegalArgumentException("The entrant is not part of the pairing");

        if (!hasEntrant(winningEntrant))
            throw new NoSuchEntrantException();
        if (!pairings.isActive(pairing))
            throw new NoSuchPairingException();

        E losingEntrant = pairing.getOther(winningEntrant);

        if (pairing.equals(firstPlacePairing)) {
            placements.setPlacement(winningEntrant, Placement.FIRST);
            placements.setPlacement(losingEntrant, Placement.SECOND);
        }
        else if (pairing.equals(thirdPlacePairing)) {
            placements.setPlacement(winningEntrant, Placement.THIRD);
            placements.setPlacement(losingEntrant, Placement.NONE);
        }

        pairings.finish(pairing);
    }

    @Override
    public Pairing<E> declareWinner(E winningEntrant)
            throws NoSuchEntrantException, MissingPairingException {

        if (!hasEntrant(winningEntrant))
            throw new NoSuchEntrantException();

        Pairing<E> pairing = getPairingForEntrant(winningEntrant);
        if (!pairings.isActive(pairing))
            throw new MissingPairingException();

        declareWinner(winningEntrant, pairing);
        return pairing;
    }

    @Override
    @UnsupportedOperation
    public void declareTie(Pairing<E> pairing) {
        throw new UnsupportedOperationException();
    }

    public void setFirstPlacePairingFirst() {
        if (pairingOrder.size() == 2) {
            pairingOrder.set(0, firstPlacePairing);
            pairingOrder.set(1, thirdPlacePairing);
        }
    }

    public void setThirdPlacePairingFirst() {
        if (pairingOrder.size() == 2) {
            pairingOrder.set(0, thirdPlacePairing);
            pairingOrder.set(1, firstPlacePairing);
        }
    }

    @Override
    public Set<E> getEntrants() {
        return Collections.unmodifiableSet(entrants);
    }

    @Override
    public Pairings<E> getPairings() {
        return pairings;
    }

    @Override
    public Set<E> getPairedEntrants() {
        return pairings.getActiveEntrants();
    }

    @Override
    public Set<Pairing<E>> getActivePairings() {
        return pairings.getActive();
    }

    @Override
    public Set<Pairing<E>> getFinishedPairings() {
        return pairings.getFinished();
    }

    @Override
    public Pairing<E> getLastPairing(E entrant) {
        return pairings.getLastPairingOfEntrant(entrant);
    }

    @Override
    public boolean hasEntrant(E entrant) {
        return entrants.contains(entrant);
    }

    @Override
    public boolean hasEntrantResult(E entrant) {
        return placements.getPlacement(entrant) != Placement.TBD;
    }

    @Override
    public boolean hasWon(E entrant) {
        Placement placement = placements.getPlacement(entrant);
        return placement == Placement.FIRST || placement == Placement.THIRD;
    }

    @Override
    public boolean hasLost(E entrant) {
        Placement placement = placements.getPlacement(entrant);
        return placement == Placement.SECOND || placement == Placement.NONE;
    }

    @Override
    public boolean hasStateAbout(E entrant) {
        return firstPlacePairing.contains(entrant) || thirdPlacePairing.contains(entrant);
    }

    @Override
    public boolean isEntrantPaired(E entrant) {
        return pairings.hasActiveEntrant(entrant);
    }

    @Override
    public boolean isFinished() {
        return pairingOrder.isEmpty() && pairings.getActive().isEmpty();
    }

//    @Override
    public List<Pairing<E>> getUpcomingPairings() {
        return Collections.unmodifiableList(pairingOrder);
    }

//    @Override
    public Collection<Pairing<E>> getOutstandingPairings() {
        return Collections.emptyList();
    }

    @Override
    public Placement getPlacement(E entrant) {
        return placements.getPlacement(entrant);
    }

    @Override
    public E getEntrantByPlacement(Placement placement) {
        return placements.getEntrantByPlacement(placement);
    }

    private Pairing<E> getPairingForEntrant(E entrant) {
        assert firstPlacePairing.contains(entrant) || thirdPlacePairing.contains(entrant);
        return firstPlacePairing.contains(entrant) ? firstPlacePairing : thirdPlacePairing;
    }
}

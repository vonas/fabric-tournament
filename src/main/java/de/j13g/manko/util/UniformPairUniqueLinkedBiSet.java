package de.j13g.manko.util;

import java.util.LinkedHashSet;

public class UniformPairUniqueLinkedBiSet<E, P extends UniformPair<E>>
        extends UniformPairUniqueBiSet<E, P> {

    public UniformPairUniqueLinkedBiSet() {
        super(new LinkedHashSet<>());
    }
}

package base.teacher.oracle;

import automaton.PTA;
import base.Compatibility;
import base.teacher.observationTree.ObservationTree;
import base.teacher.oracle.EquivalenceOracle;
import base.teacher.oracle.RandomTesting;
import base.teacher.oracle.conv.ConvergenceCriterion;
import base.teacher.oracle.conv.TreeBasedDistributionCexFinder;
import trace.ResetTimedIncompleteTrace;
import trace.ResetTimedTrace;
import utils.FastImmPair;

import java.util.Map;
import java.util.Optional;

public class Convergence extends EquivalenceOracle {
    private int maxTries;

    private final ConvergenceCriterion criterion;
    private final TreeBasedDistributionCexFinder refiner;

    public Convergence(RandomTesting samplingMachine,
                       ObservationTree tree, Compatibility compatibilityChecker,
                       ConvergenceCriterion criterion) {
        super(samplingMachine, tree, compatibilityChecker);
        this.criterion = criterion;
        this.refiner = new TreeBasedDistributionCexFinder(compatibilityChecker);
    }

    @Override
    public void init(Map<String, Object> parameters) {
        this.maxTries = (int) parameters.get("maxTries");
    }

    @Override
    public FastImmPair<Boolean, ResetTimedIncompleteTrace> findCex(int rounds, PTA hypo, double unambiguousRatio) {
        if (isChaosReachable(hypo))
            return FastImmPair.of(false, null);
        boolean isEquivalent = criterion.converged(rounds, hypo, unambiguousRatio);
        if (isEquivalent)
            return FastImmPair.of(true, null);
        Optional<ResetTimedTrace> cex = samplingMachine.sampleForFindCex(hypo, maxTries);
        if (cex.isPresent())
            return FastImmPair.of(false, (ResetTimedIncompleteTrace) cex.get().dropLastOutput());
        cex = refiner.findCex(tree, hypo);
        return cex.map(trace -> FastImmPair.of(false, (ResetTimedIncompleteTrace) trace.dropLastOutput())).orElseGet(() -> FastImmPair.of(false, null));
    }
}

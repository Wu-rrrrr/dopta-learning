package base.teacher.oracle;

import automaton.Location;
import automaton.Output;
import automaton.PTA;
import automaton.Transition;
import base.Compatibility;
import base.teacher.observationTree.ObservationTree;
import lombok.AllArgsConstructor;
import lombok.Data;
import trace.ResetTimedIncompleteTrace;
import utils.FastImmPair;

import java.util.List;
import java.util.Map;
import java.util.Random;

@AllArgsConstructor
public abstract class EquivalenceOracle {

    protected RandomTesting samplingMachine;
    protected ObservationTree tree;
    protected Compatibility compatibilityChecker;

    public abstract void init(Map<String, Object> parameter);
    public abstract FastImmPair<Boolean, ResetTimedIncompleteTrace> findCex(int rounds, PTA hypo, double unambiguousRatio);

    protected boolean isChaosReachable(PTA model) {
        Output chaosLabel = Output.chaos();
        for (Location location : model.getLocations()) {
            if (!location.getLabel().equals(chaosLabel)) {
                for (Transition t : location.getAllTransitions()) {
                    if (t.getTarget().getLabel().equals(chaosLabel)) {
                        System.out.println("chaos location is reachable");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

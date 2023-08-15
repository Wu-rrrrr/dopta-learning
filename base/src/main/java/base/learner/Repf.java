package base.learner;

import automaton.Guard;
import automaton.Input;
import automaton.Location;
import automaton.PTA;
import lombok.NoArgsConstructor;
import trace.ResetTimedIncompleteTrace;
import trace.ResetTimedTrace;
import trace.TimedInput;
import utils.FastImmPair;

import java.util.HashMap;
import java.util.Map;

public class Repf {
    private Map<Location, Map<FastImmPair<Input, Guard>, ResetTimedIncompleteTrace>> repf;

    public Repf() {
        repf = new HashMap<>();
    }

    public void put(Location q, Input i, Guard g, ResetTimedIncompleteTrace resetTimedIncompleteTrace) {
        Map<FastImmPair<Input, Guard>, ResetTimedIncompleteTrace> value = repf.get(q);
        if (value == null) {
            value = new HashMap<>();
        }
        value.put(FastImmPair.of(i, g), resetTimedIncompleteTrace);
        repf.put(q, value);
    }

    public ResetTimedIncompleteTrace get(ResetTimedIncompleteTrace incompleteTrace, PTA hypo) {
        ResetTimedTrace trace = incompleteTrace.getTrace();
        TimedInput lastInput = incompleteTrace.get(incompleteTrace.length()-1).right;

        Location location = hypo.getStateReachedByResetLogicalTimedTrace(trace).left;

        Map<FastImmPair<Input, Guard>, ResetTimedIncompleteTrace> value = repf.get(location);
        if (value == null) {
            System.out.println("Repf error");
            System.exit(0);
            return null;
        }
        for (FastImmPair<Input, Guard> item : value.keySet()) {
            if (item.left.equals(lastInput.getInput()) && item.right.enableAction(lastInput.getClockVal())) {
                return value.get(item);
            }
        }

        System.out.println("Repf error");
        System.exit(0);
        return null;
    }
}

package trace;

import automaton.Output;
import trace.base.SuffixTrace;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;

public class ResetTimedSuffixTrace extends SuffixTrace<TimedOutput> {

    public ResetTimedSuffixTrace(TimedInput firstInput, List<FastImmPair<TimedOutput, TimedInput>> trace) {
        super(firstInput, trace);
    }

    public ResetTimedSuffixTrace prepend(FastImmPair<TimedInput, TimedOutput> symbol) {
        List<FastImmPair<TimedOutput,TimedInput>> newTrace = new ArrayList<>();
        newTrace.add(FastImmPair.of(symbol.getRight(), getFirstInput()));
        if (getTrace() != null && !getTrace().isEmpty()) {
            newTrace.addAll(getTrace());
        }
        return new ResetTimedSuffixTrace(symbol.getLeft(), newTrace);
    }

    public TimedSuffixTrace convert() {
        List<FastImmPair<Output, TimedInput>> steps = new ArrayList<>();
        for (FastImmPair<TimedOutput, TimedInput> step : getTrace()) {
            steps.add(FastImmPair.of(step.left.getOutput(), step.right));
        }
        return new TimedSuffixTrace(getFirstInput(), steps);
    }

    public static ResetTimedSuffixTrace empty(TimedInput first) {
        return new ResetTimedSuffixTrace(first, new ArrayList<>());
    }
}

package trace;

import automaton.Output;
import trace.base.SuffixTrace;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;

public class TimedSuffixTrace extends SuffixTrace<Output> {

    public TimedSuffixTrace(TimedInput firstInput, List<FastImmPair<Output, TimedInput>> trace) {
        super(firstInput, trace);
    }

    public TimedSuffixTrace prepend(FastImmPair<TimedInput, Output> symbol) {
        List<FastImmPair<Output,TimedInput>> newTrace = new ArrayList<>();
        newTrace.add(FastImmPair.of(symbol.getRight(), getFirstInput()));
        if (getTrace() != null && !getTrace().isEmpty()) {
            newTrace.addAll(getTrace());
        }
        return new TimedSuffixTrace(symbol.getLeft(), newTrace);
    }

    public static TimedSuffixTrace empty(TimedInput first) {
        return new TimedSuffixTrace(first, new ArrayList<>());
    }
}

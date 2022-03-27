package trace;

import automaton.Output;
import trace.base.IncompleteTrace;
import trace.base.Trace;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;

public class TimedTrace extends Trace<Output> {
    public TimedTrace(Output firstOutput, List<FastImmPair<TimedInput, Output>> trace) {
        super(firstOutput, trace);
    }

    public TimedTrace append(FastImmPair<TimedInput, Output> symbol) {
        List<FastImmPair<TimedInput, Output>> newTrace = new ArrayList<>();
        if (getTrace() != null && !getTrace().isEmpty()) {
            newTrace.addAll(getTrace());
        }
        newTrace.add(symbol);
        return new TimedTrace(getFirstOutput(), newTrace);
    }

    public TimedTrace prefix(int length) {
        if (length > length())
            return null;
        if (length == 1)
            return new TimedTrace(getFirstOutput(), new ArrayList<>());
        List<FastImmPair<TimedInput, Output>> prefixTrace = new ArrayList<>(getTrace().subList(0, length-1));
        return new TimedTrace(getFirstOutput(), prefixTrace);
    }

    public TimedIncompleteTrace dropLastOutput() {
        List<FastImmPair<Output, TimedInput>> newTrace = new ArrayList<>();
        if(getTrace().size() == 0)
            return new TimedIncompleteTrace(newTrace);
        Output output = getFirstOutput();
        for (FastImmPair<TimedInput, Output> pair : getTrace()) {
            newTrace.add(FastImmPair.of(output, pair.left));
            output = pair.right;
        }
        return new TimedIncompleteTrace(newTrace);
    }

    public Output lastOutput() {
        if (super.getTrace().size() == 0)
            return super.getFirstOutput();
        return super.getTrace().get(super.getTrace().size() - 1).getRight();
    }

    public static TimedTrace empty(Output first) {
        return new TimedTrace(first, new ArrayList<>());
    }

    public Output getLastOutput() {
        return lastOutput();
    }
}

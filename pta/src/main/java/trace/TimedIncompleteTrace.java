package trace;

import automaton.Output;
import org.apache.commons.lang3.tuple.Triple;
import trace.base.IncompleteTrace;
import trace.base.SuffixTrace;
import trace.base.Trace;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;

public class TimedIncompleteTrace extends IncompleteTrace<Output> {

    public TimedIncompleteTrace(List<FastImmPair<Output, TimedInput>> trace) {
        super(trace);
    }

    public TimedIncompleteTrace(Trace<Output> prefixTrace, SuffixTrace<Output> suffixTrace) {
        super(prefixTrace, suffixTrace);
    }

    public TimedIncompleteTrace(Trace<Output> prefixTrace, TimedInput lastInput) {
        super(prefixTrace, TimedSuffixTrace.empty(lastInput));
    }

    public TimedIncompleteTrace append(FastImmPair<Output, TimedInput> succSymbol) {
        List<FastImmPair<Output, TimedInput>> newTrace = new ArrayList<>();
        if (getPairs() != null && !getPairs().isEmpty()) {
            newTrace.addAll(getPairs());
        }
        newTrace.add(succSymbol);
        return new TimedIncompleteTrace(newTrace);
    }

    public TimedIncompleteTrace prefix(Integer prefixL) {
        if (prefixL > getPairs().size())
            return new TimedIncompleteTrace(new ArrayList<>());
        return new TimedIncompleteTrace(new ArrayList<>(getPairs().subList(0, prefixL)));
    }

    public TimedTrace getTrace() {
        if (getPairs() == null || getPairs().isEmpty())
            return null;
        TimedTrace trace = new TimedTrace(getPairs().get(0).left, new ArrayList<>());
        TimedInput input = getPairs().get(0).right;
        for (int i = 1; i < getPairs().size(); i++) {
            trace.getTrace().add(FastImmPair.of(input, getPairs().get(i).left));
            input = getPairs().get(i).right;
        }
        return trace;
    }

    public Triple<TimedTrace, FastImmPair<TimedInput, Output>, TimedSuffixTrace> splitAt(int pos) {
        if(getPairs().size() <= 1 || pos+1 >= getPairs().size())
            return null;

        TimedTrace trace = getTrace();
        FastImmPair<TimedInput, Output> middle = trace.get(pos);
        TimedTrace prefix = trace.prefix(pos+1);
        TimedSuffixTrace suffix = new TimedSuffixTrace(get(length()-1).right, new ArrayList<>());
        for (int i = trace.length()-2; i > pos; i--) {
            suffix.prepend(trace.get(i));
        }

        return Triple.of(prefix, middle, suffix);
    }

    public static TimedIncompleteTrace empty() {
        return new TimedIncompleteTrace(new ArrayList<>());
    }

    public List<FastImmPair<Output, TimedInput>> getSteps() {
        return super.getPairs();
    }
}

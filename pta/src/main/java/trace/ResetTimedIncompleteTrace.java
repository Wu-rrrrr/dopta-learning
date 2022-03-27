package trace;

import automaton.Output;
import org.apache.commons.lang3.tuple.Triple;
import trace.base.IncompleteTrace;
import trace.base.SuffixTrace;
import trace.base.Trace;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;

public class ResetTimedIncompleteTrace extends IncompleteTrace<TimedOutput> {
    public ResetTimedIncompleteTrace(List<FastImmPair<TimedOutput, TimedInput>> pairs) {
        super(pairs);
    }

    public ResetTimedIncompleteTrace(Output init, TimedInput firstInput) {
        super(new ArrayList<>());
        TimedOutput initialOutput = TimedOutput.create(true, init.getSymbol());
        getPairs().add(FastImmPair.of(initialOutput, firstInput));
    }

    public ResetTimedIncompleteTrace(Trace<TimedOutput> prefixTrace, SuffixTrace<TimedOutput> suffixTrace) {
        super(prefixTrace, suffixTrace);
    }

    public ResetTimedIncompleteTrace(ResetTimedTrace prefix, TimedInput lastInput) {
        super(prefix, ResetTimedSuffixTrace.empty(lastInput));
    }

    public ResetTimedIncompleteTrace append(FastImmPair<TimedOutput, TimedInput> succSymbol) {
        List<FastImmPair<TimedOutput, TimedInput>> newTrace = new ArrayList<>();
        if (getPairs() != null && !getPairs().isEmpty()) {
            newTrace.addAll(getPairs());
        }
        newTrace.add(succSymbol);
        return new ResetTimedIncompleteTrace(newTrace);
    }

    public ResetTimedIncompleteTrace prefix(Integer prefixL) {
        if (prefixL > getPairs().size())
            return new ResetTimedIncompleteTrace(new ArrayList<>());
        return new ResetTimedIncompleteTrace(new ArrayList<>(getPairs().subList(0, prefixL)));
    }

    public ResetTimedTrace getTrace() {
        if (getPairs() == null || getPairs().isEmpty())
            return null;
        ResetTimedTrace trace = new ResetTimedTrace(getPairs().get(0).left.getOutput(), new ArrayList<>());
        TimedInput input = getPairs().get(0).right;
        for (int i = 1; i < getPairs().size(); i++) {
            trace.getTrace().add(FastImmPair.of(input, getPairs().get(i).left));
            input = getPairs().get(i).right;
        }
        return trace;
    }

    public Triple<ResetTimedTrace, FastImmPair<TimedInput, TimedOutput>, ResetTimedSuffixTrace> splitAt(int pos) {
        if(getPairs().size() <= 1 || pos+1 >= getPairs().size())
            return null;

        ResetTimedTrace trace = getTrace();
        FastImmPair<TimedInput, TimedOutput> middle = trace.get(pos);
        ResetTimedTrace prefix = trace.prefix(pos+1);
        ResetTimedSuffixTrace suffix = new ResetTimedSuffixTrace(get(length()-1).right, new ArrayList<>());
        for (int i = trace.length()-2; i > pos; i--) {
            suffix.prepend(trace.get(i));
        }

        return Triple.of(prefix, middle, suffix);
    }

    public static ResetTimedIncompleteTrace empty() {
        return new ResetTimedIncompleteTrace(new ArrayList<>());
    }

    public List<FastImmPair<TimedOutput, TimedInput>> getSteps() {
        return getPairs();
    }
}

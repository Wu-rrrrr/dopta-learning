package trace.base;

import automaton.Output;
import automaton.Transition;
import lombok.AllArgsConstructor;
import lombok.Data;
import trace.TimedInput;
import trace.TimedOutput;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class Trace<T> {
    @Override
    public String toString() {
        return firstOutput+ "_" + trace.stream().map(p -> p.getLeft().toString() + "_" + p.getRight().toString())
                .collect(Collectors.joining("_"));
    }

    private T firstOutput;
    private List<FastImmPair<TimedInput, T>> trace;

    public Trace(IncompleteTrace<T> incompleteTrace, T lastOutput) {
        if (incompleteTrace.length() == 0) {
            firstOutput = lastOutput;
            trace = new ArrayList<>();
        } else {
            firstOutput = incompleteTrace.getIthOutput(0);
            trace = new ArrayList<>();
            TimedInput input = incompleteTrace.get(0).right;
            for (int i = 1; i < incompleteTrace.length(); i++) {
                trace.add(FastImmPair.of(input, incompleteTrace.get(i).left));
                input = incompleteTrace.get(i).right;
            }
            trace.add(FastImmPair.of(input, lastOutput));
        }
    }

    public T getFirstOutput() {
        return firstOutput;
    }

    public boolean isExtensionOf(Trace<T> accSeq) {
        if (length() != accSeq.length() + 1 || !firstOutput.equals(accSeq.firstOutput))
            return false;
        for (int pos = 0; pos < accSeq.length()-1; pos++) {
            if (!get(pos).equals(accSeq.get(pos)))
                return false;
        }
        return true;
    }

    public boolean isExtensionOf(Trace<T> accSeq, TimedInput timedInput) {
        return isExtensionOf(accSeq) && get(length() - 2).left.equals(timedInput);
    }

    public FastImmPair<TimedInput, T> get(int pos) {
        return trace.get(pos);
    }

    public int length() {
        return trace.size()+1;
    }

    public IncompleteTrace<T> dropLastOutput() {
        List<FastImmPair<T, TimedInput>> newTrace = new ArrayList<>();
        if(trace.size() == 0)
            return new IncompleteTrace<>(newTrace);
        T output = firstOutput;
        for (FastImmPair<TimedInput, T> pair : trace) {
            newTrace.add(FastImmPair.of(output, pair.left));
            output = pair.right;
        }
        return new IncompleteTrace<>(newTrace);
    }

    public T getLastOutput () {
        if (trace == null || trace.isEmpty())
            return firstOutput;
        return trace.get(trace.size()-1).right;
    }

    public TimedInput getIthInput(int i) {
        if (i < 1 || i - 1 > trace.size()) {
            return null;
        }
        return trace.get(i-1).left;
    }

    public boolean isEmpty() {
        return trace.isEmpty();
    }
}

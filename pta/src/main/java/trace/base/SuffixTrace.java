package trace.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import trace.TimedInput;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class SuffixTrace<T> {
    private TimedInput firstInput;
    private List<FastImmPair<T,TimedInput>> trace;

    @Override
    public String toString() {
        if (firstInput == null) {
            return "epsilon";
        }
        return firstInput + "_" + trace.stream().map(p -> p.getLeft().toString() + "_" + p.getRight().toString())
                .collect(Collectors.joining("_"));
    }

    public SuffixTrace<T> prepend(FastImmPair<TimedInput, T> symbol) {
        List<FastImmPair<T,TimedInput>> newTrace = new ArrayList<>();
        newTrace.add(FastImmPair.of(symbol.getRight(), firstInput));
        if (trace != null && !trace.isEmpty()) {
            newTrace.addAll(trace);
        }
        return new SuffixTrace<>(symbol.getLeft(), newTrace);
    }

    public TimedInput getFirstInput() {
        return firstInput;
    }

    public int length() {
        return trace.size()+1;
    }

    public FastImmPair<T, TimedInput> get(int i) {
        return trace.get(i);
    }
}

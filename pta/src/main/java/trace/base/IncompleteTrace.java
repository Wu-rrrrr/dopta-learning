package trace.base;

import automaton.Output;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.tuple.Triple;
import trace.TimedInput;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class IncompleteTrace<T> {

    private List<FastImmPair<T,TimedInput>> pairs = new ArrayList<>();

    @Override
    public String toString() {
        return pairs.stream().map(p -> p.getLeft().toString() + "_" + p.getRight().toString())
                .collect(Collectors.joining("_"));
    }

    public IncompleteTrace(Trace<T> prefixTrace, SuffixTrace<T> suffixTrace) {
        if (prefixTrace.length() == 1) {
            pairs = new ArrayList<>();
            pairs.add(FastImmPair.of(prefixTrace.getFirstOutput(), suffixTrace.getFirstInput()));
        } else {
            pairs = prefixTrace.dropLastOutput().pairs;
            pairs.add(FastImmPair.of(prefixTrace.getLastOutput(), suffixTrace.getFirstInput()));
        }
        pairs.addAll(suffixTrace.getTrace());
    }

    public int length() {
        return pairs.size();
    }

    public FastImmPair<T, TimedInput> get(int i) {
        return pairs.get(i);
    }

    public boolean prefixOf(IncompleteTrace<T> other) {
        if(length() > other.length())
            return false;
        for(int i = 0; i < length(); i++){
            if(!get(i).equals(other.get(i)))
                return false;
        }
        return true;
    }

    public T getIthOutput(int i) {
        return get(i).getLeft();
    }
}

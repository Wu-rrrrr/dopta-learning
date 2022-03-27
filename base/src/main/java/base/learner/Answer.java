package base.learner;

import base.Compatibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.tuple.Triple;
import trace.TimedOutput;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class Answer {
    private boolean complete = false;
    private List<Boolean> resets = new ArrayList<>();
    private Map<TimedOutput, Integer> frequencies = new HashMap<>();

    public Answer(Triple<List<Boolean>, Map<TimedOutput, Integer>, Boolean> outputFrequenciesAndCompleteness) {
        this.resets = outputFrequenciesAndCompleteness.getLeft();
        this.frequencies = outputFrequenciesAndCompleteness.getMiddle();
        this.complete = outputFrequenciesAndCompleteness.getRight();
    }

    @Override
    public String toString() {
        return "Answer [complete=" + complete + ", resets=" + resets + ", frequencies=" + frequencies + "]";
    }

    public boolean answerEqual(Answer other, Compatibility compChecker) {
        if (!isComplete() || !other.isComplete()) {
            return true;
        }
        if (!resets.equals(other.resets))
            return false;
        return compChecker.compatible(frequencies, other.frequencies);
    }

    public void setFrequencies(Triple<List<Boolean>, Map<TimedOutput, Integer>, Boolean> outputFrequenciesAndCompleteness) {
        resets = outputFrequenciesAndCompleteness.getLeft();
        frequencies = outputFrequenciesAndCompleteness.getMiddle();
        complete = outputFrequenciesAndCompleteness.getRight();
    }
}

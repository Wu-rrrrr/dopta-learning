package base.learner;

import base.Compatibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Triple;
import trace.TimedOutput;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Answer {
    private boolean complete = false;
    private boolean valid = true;
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
        if (!isValid() || !other.isValid()) {
            return true;
        }
        if (!isComplete() || !other.isComplete()) {
            return true;
        }
        if (!resets.equals(other.resets))
            return false;
        return compChecker.compatible(frequencies, other.frequencies);
    }

    public static Answer InvalidAnswer() {
        return new Answer(true, false, new ArrayList<>(), new HashMap<>());
    }

    public static Answer ValidAnswer() {
        return new Answer(false, true, new ArrayList<>(), new HashMap<>());
    }

    public static Answer setValidAnswer(List<Boolean> resets, Map<TimedOutput, Integer> freq, boolean complete) {
        return new Answer(complete, true, resets, freq);
    }
}

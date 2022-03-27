package trace;

import automaton.Output;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import trace.base.IncompleteTrace;
import trace.base.Trace;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;

public class ResetTimedTrace extends Trace<TimedOutput> {

    public ResetTimedTrace(Output firstOutput, List<FastImmPair<TimedInput, TimedOutput>> trace) {
        super(TimedOutput.create(true, firstOutput.getSymbol()), trace);
    }

    public ResetTimedTrace(ResetTimedIncompleteTrace incompleteTrace, TimedOutput lastOutput) {
        super(incompleteTrace, lastOutput);
    }

    public ResetTimedTrace(TimedOutput firstOutput, List<FastImmPair<TimedInput, TimedOutput>> trace) {
        super(firstOutput, trace);
    }

    public ResetTimedTrace append(FastImmPair<TimedInput, TimedOutput> symbol) {
        List<FastImmPair<TimedInput, TimedOutput>> newTrace = new ArrayList<>();
        if (getTrace() != null && !getTrace().isEmpty()) {
            newTrace.addAll(getTrace());
        }
        newTrace.add(symbol);
        return new ResetTimedTrace(getFirstOutput().getOutput(), newTrace);
    }

    public ResetTimedTrace prefix(int length) {
        if (length > length())
            return null;
        if (length == 1)
            return new ResetTimedTrace(getFirstOutput().getOutput(), new ArrayList<>());
        List<FastImmPair<TimedInput, TimedOutput>> prefixTrace = new ArrayList<>(getTrace().subList(0, length-1));
        return new ResetTimedTrace(getFirstOutput().getOutput(), prefixTrace);
    }

    public ResetTimedTrace suffix(int pos) {
        if (pos >= length()) {
            return null;
        }
        if (pos == 0) {
            return new ResetTimedTrace(getFirstOutput().getOutput(), new ArrayList<>(getTrace()));
        }
        TimedOutput newFirstOutput = getTrace().get(pos-1).right;
        List<FastImmPair<TimedInput, TimedOutput>> newTrace = getTrace().subList(pos, getTrace().size());
        return new ResetTimedTrace(newFirstOutput, newTrace);
    }

    public ResetTimedIncompleteTrace dropLastOutput() {
        List<FastImmPair<TimedOutput, TimedInput>> newTrace = new ArrayList<>();
        if(getTrace().size() == 0)
            return new ResetTimedIncompleteTrace(newTrace);
        TimedOutput output = getFirstOutput();
        for (FastImmPair<TimedInput, TimedOutput> pair : getTrace()) {
            newTrace.add(FastImmPair.of(output, pair.left));
            output = pair.right;
        }
        return new ResetTimedIncompleteTrace(newTrace);
    }

    public Output lastOutput() {
        List<FastImmPair<TimedInput, TimedOutput>> trace = super.getTrace();
        if (trace.size() == 0)
            return super.getFirstOutput().getOutput();
        return trace.get(trace.size() - 1).getRight().getOutput();
    }

    public static ResetTimedTrace empty(Output first) {
        return new ResetTimedTrace(first, new ArrayList<>());
    }

    public TimedTrace convert() {
        List<FastImmPair<TimedInput, Output>> timeSteps = new ArrayList<>();
        for (FastImmPair<TimedInput, TimedOutput> steps : getTrace()) {
            timeSteps.add(FastImmPair.of(steps.left, steps.right.getOutput()));
        }
        return new TimedTrace(getFirstOutput().getOutput(), timeSteps);
    }

    public ResetTimedTrace transformLogicalTimedTrace() {
        double currentLogicalTime = 0.0;
        List<FastImmPair<TimedInput, TimedOutput>> timeSteps = new ArrayList<>();
        for (FastImmPair<TimedInput, TimedOutput> delayStep : getTrace()) {
            currentLogicalTime = currentLogicalTime + delayStep.left.getClockVal();
            timeSteps.add(FastImmPair.of(TimedInput.create(delayStep.left.getInput().getSymbol(), currentLogicalTime), delayStep.right));
            if (delayStep.right.isReset()) {
                currentLogicalTime = 0.0;
            }
        }
        return new ResetTimedTrace(getFirstOutput().getOutput(), timeSteps);
    }
}

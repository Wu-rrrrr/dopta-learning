package evaluation;

import automaton.Location;
import automaton.Output;
import automaton.PTA;
import automaton.Transition;
import base.Compatibility;
import lombok.Data;
import trace.*;
import utils.FastImmPair;

import java.util.*;

@Data
public class SampleTest {
    private final Random random;
    private final PTA target;

    private double passRatio;
    private double klDivergence;

    private int completeThreshold = 50;
    private Compatibility compChecker;

    public SampleTest(PTA target) {
        this.target = target;
        random = new Random();
    }

    private double computeDivergence (Map<TimedOutput, Double> d1, Map<TimedOutput, Double> d2) {
        double res = 0.0;
        for (TimedOutput edge : d2.keySet()) {
            double p1 = d1.get(edge);
            double p2 = d2.get(edge);
            res += p1 * Math.log(p1 / p2);
        }
        return res;
    }

    private TimedTrace sample (int bound) {
        Output initLabel = target.getInitial().getLabel();
        TimedTrace delayTimedTrace = new TimedTrace(initLabel, new ArrayList<>());
        Location currentLoc = target.getInitial();
        double currentClock = 0.0;
        Set<Transition> visited = new HashSet<>();
        List<Transition> validSuccessors = currentLoc.getAllTransitions();
        while (!validSuccessors.isEmpty()) {
            int index = random.nextInt(validSuccessors.size());
            Transition selectedSucc = validSuccessors.get(index);
            visited.add(selectedSucc);

            double logicalClockValuation = selectedSucc.getGuard().getRandomValue(currentClock, bound);

            delayTimedTrace = delayTimedTrace.append(FastImmPair.of(TimedInput.create(selectedSucc.getInput().getSymbol(),
                    logicalClockValuation - currentClock),
                    Output.create(selectedSucc.getTarget().getLabel().getSymbol())));
            currentLoc = selectedSucc.getTarget();
            currentClock = selectedSucc.isReset() ? 0.0 : logicalClockValuation;

            validSuccessors = new ArrayList<>();
            for (Transition t : currentLoc.getAllTransitions()) {
                if (t.getGuard().getMaxBound() > currentClock && !visited.contains(t)) {
                    validSuccessors.add(t);
                }
            }
        }
        return delayTimedTrace;
    }

    public void evaluatePassRatio(PTA hypothesis, int sampleNum, int bound) {
        int notPass = 0;
        int samples = 0;
        while (samples <= sampleNum) {
            TimedTrace delayTimedTrace = sample(bound);
            for (int i = 2; i <= delayTimedTrace.length(); i++) {
                samples++;
                TimedTrace prefix = delayTimedTrace.prefix(i - 1);
                TimedInput input = delayTimedTrace.get(i - 2).left;
                if (hypothesis.getStateReachedByDelayTimedTrace(prefix) == null) {
//                    System.out.printf("unreachable trace: %s\n", prefix);
                    notPass += 1;
                    continue;
                }
                FastImmPair<Location, Double> tarReachedState = target.getStateReachedByDelayTimedTrace(prefix);
                FastImmPair<Location, Double> hypReachedState = hypothesis.getStateReachedByDelayTimedTrace(prefix);
                Map<TimedOutput, Double> tarDistribution = target.getDistribution(tarReachedState.left,
                        tarReachedState.right, input);
                Map<TimedOutput, Double> hypDistribution = hypothesis.getDistribution(hypReachedState.left,
                        hypReachedState.right, input);
                Map<TimedOutput, Integer> tarFreq = new HashMap<>();
                Map<TimedOutput, Integer> hypoFreq = new HashMap<>();
                for (int j = 0; j < completeThreshold; j++) {
                    TimedOutput timedOutput = outputByDistribution(random, tarDistribution);
                    tarFreq.putIfAbsent(timedOutput, 0);
                    int freq = tarFreq.get(timedOutput) + 1;
                    tarFreq.put(timedOutput, freq);
                }
                for (int j = 0; j < completeThreshold; j++) {
                    TimedOutput timedOutput = outputByDistribution(random, hypDistribution);
                    hypoFreq.putIfAbsent(timedOutput, 0);
                    int freq = hypoFreq.get(timedOutput) + 1;
                    hypoFreq.put(timedOutput, freq);
                }
                if (!compChecker.compatible(tarFreq, hypoFreq)) {
                    notPass += 1;
                }
            }
        }
        passRatio = 1 - notPass / (double)samples;
    }

    private TimedOutput outputByDistribution(Random random, Map<TimedOutput, Double> dist) {
        double value = random.nextDouble();
        double startPoint = 0.0;
        for (Map.Entry<TimedOutput, Double> edge : dist.entrySet()) {
            double endPoint = startPoint + edge.getValue();
            if (value >= startPoint && value < endPoint) {
                return edge.getKey();
            }
            startPoint = endPoint;
        }
        return null;
    }
}

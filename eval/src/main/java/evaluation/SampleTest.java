package evaluation;

import automaton.Location;
import automaton.Output;
import automaton.PTA;
import automaton.Transition;
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
        Set<Location> visited = new HashSet<>();
        Output initLabel = target.getInitial().getLabel();
        TimedTrace delayTimedTrace = new TimedTrace(initLabel, new ArrayList<>());
        Location currentLoc = target.getInitial();
        double currentClock = 0.0;
        while (!visited.contains(currentLoc)) {
            visited.add(currentLoc);
            List<Transition> validSuccessors = new ArrayList<>();
            for (Transition t : currentLoc.getAllTransitions()) {
                if (t.getGuard().getMaxBound() > currentClock) {
                    validSuccessors.add(t);
                }
            }
            if (validSuccessors.isEmpty())
                return delayTimedTrace;

            int index = random.nextInt(validSuccessors.size());
            Transition selectedSucc = validSuccessors.get(index);

            double logicalClockValuation = selectedSucc.getGuard().getRandomValue(currentClock, bound);

            delayTimedTrace = delayTimedTrace.append(FastImmPair.of(TimedInput.create(selectedSucc.getInput().getSymbol(), logicalClockValuation - currentClock),
                    Output.create(selectedSucc.getTarget().getLabel().getSymbol())));
            currentLoc = selectedSucc.getTarget();
            currentClock = selectedSucc.isReset() ? 0.0 : logicalClockValuation;
        }
        return delayTimedTrace;
    }

    public void evaluatePassRatio(PTA hypothesis, int sampleNum, int bound) {
        klDivergence = 0;
        int notPass = 0;
        for (int i = 0; i < sampleNum; i++) {
            TimedTrace delayTimedTrace = sample(bound);
            if (hypothesis.getStateReachedByDelayTimedTrace(delayTimedTrace) == null) {
                System.out.printf("unreachable trace: %s\n", delayTimedTrace);
                notPass += 1;
                continue;
            }
            FastImmPair<Location, Double> tarReachedState = target.getStateReachedByDelayTimedTrace(delayTimedTrace);
            FastImmPair<Location, Double> hypReachedState = hypothesis.getStateReachedByDelayTimedTrace(delayTimedTrace);
            Map<TimedOutput, Double> tarDistribution = target.getDistribution(tarReachedState.left,
                    tarReachedState.right, delayTimedTrace.getIthInput(delayTimedTrace.length()-1));
            Map<TimedOutput, Double> hypDistribution = hypothesis.getDistribution(hypReachedState.left,
                    hypReachedState.right, delayTimedTrace.getIthInput(delayTimedTrace.length()-1));

            if (!tarDistribution.keySet().equals(hypDistribution.keySet())) {
                System.out.printf("unreachable trace: %s\n", delayTimedTrace);
                notPass += 1;
                continue;
            }
            klDivergence += computeDivergence(hypDistribution, tarDistribution);
        }
        passRatio = 1 - notPass / (double)sampleNum;
    }
}

package base.learner;

import automaton.*;
import base.Compatibility;
import com.google.common.collect.BoundType;
import trace.ResetTimedIncompleteTrace;
import utils.FastImmPair;
import utils.Frequencies;

import java.io.IOException;
import java.util.*;

public interface Learner {
    //初始化
    void init(Output initialOutput);

    //学习
    void learn(LearningSetting setting) throws IOException;

    //对反例进行处理
    void processCounterexample(ResetTimedIncompleteTrace counterExample);

    void show();

    boolean check(ResetTimedIncompleteTrace counterExample);

    //构造假设自动机
    PTA buildHypothesis();

    //获取最终结果自动机
    PTA getFinalHypothesis();

    default void constructTransitions (Location chaosLocation, Compatibility compChecker, Location source, Map<Input, List<Double>> chaosClockValuations,
                                              Map<Input, Set<FastImmPair<Double, Map<Edge, Integer>>>> discreteTransitions,
                                              Map<Input, Set<Map<Edge, Integer>>> frequencySets) {
        Set<Input> inputs = frequencySets.keySet();
        Frequencies<Edge> chaosDistribution = new Frequencies<>();
        chaosDistribution.getActualFrequencies().put(new Edge(true, chaosLocation), FastImmPair.of(null, 1.0));

        for (Input input : inputs) {
            Map<Double, Frequencies<Edge>> representDistributions = new HashMap<>();
            for (double clockValuation : chaosClockValuations.get(input)) {
                representDistributions.put(clockValuation, chaosDistribution);
            }
            Map<Map<Edge, Integer>, Frequencies<Edge>> representDistributionMap = getRepresentDistributionRelation(compChecker, frequencySets.get(input));
            for (FastImmPair<Double, Map<Edge, Integer>> discreteTransition : discreteTransitions.get(input)) {
                representDistributions.put(discreteTransition.left, representDistributionMap.get(discreteTransition.right));
            }
            Map<Frequencies<Edge>, Guard> distributionGuardMap = getDistributionGuardRelation(representDistributions);
            for (Map.Entry<Frequencies<Edge>, Guard> entry : distributionGuardMap.entrySet()) {
                for (Map.Entry<Edge, FastImmPair<Integer, Double>> edge : entry.getKey().getActualFrequencies().entrySet()) {
                    source.addTransition(entry.getValue(), input, edge.getValue().right, edge.getKey().isReset(), edge.getKey().getTarget(), edge.getValue().left);
                }
            }
        }
    }

    private Map<Map<Edge, Integer>, Frequencies<Edge>> getRepresentDistributionRelation(Compatibility compChecker, Set<Map<Edge, Integer>> freqs) {
        Map<Map<Edge, Integer>, Frequencies<Edge>> map = new HashMap<>();
        List<Map<Edge, Integer>> freqList = new ArrayList<>(freqs);

        while (!freqList.isEmpty()) {
            freqList.sort(new Comparator<Map<Edge, Integer>>() {
                @Override
                public int compare(Map<Edge, Integer> o1, Map<Edge, Integer> o2) {
                    int nrObservationsOverall1 = o1.values().stream().mapToInt(Integer::intValue).sum();
                    int nrObservationsOverall2 = o2.values().stream().mapToInt(Integer::intValue).sum();
                    return Integer.compare(nrObservationsOverall2, nrObservationsOverall1);
                }
            });

            Frequencies<Edge> representFreq = new Frequencies<>(freqList.remove(0));
            representFreq.setDistribution();

            for (Map<Edge, Integer> freq : freqs) {
                if (compChecker.compatible(freq, representFreq.getFrequencies())) {
                    map.put(freq, representFreq);
                    freqList.remove(freq);
                }
            }
            freqs = new HashSet<>(freqList);
        }

        return map;
    }

    private Map<Frequencies<Edge>, Guard> getDistributionGuardRelation(Map<Double, Frequencies<Edge>> representDistribution) {
        Map<Frequencies<Edge>, List<Interval>> distributionInterviewsMap = new HashMap<>();
        List<Double> clockValuations = new ArrayList<>(representDistribution.keySet());
        clockValuations.sort(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(o1, o2);
            }
        });
        for (int i = 0; i < clockValuations.size(); i++) {
            Frequencies<Edge> distribution = representDistribution.get(clockValuations.get(i));
            Interval interval;
            if (i+1 < clockValuations.size()) {
                interval = Interval.create(clockValuations.get(i), clockValuations.get(i+1));
            } else {
                interval = Interval.create(clockValuations.get(i));
            }
            distributionInterviewsMap.putIfAbsent(distribution, new ArrayList<>());
            distributionInterviewsMap.get(distribution).add(interval);
        }

        Map<Frequencies<Edge>, Guard> distributionGuardMap = new HashMap<>();
        for (Map.Entry<Frequencies<Edge>, List<Interval>> entry :
                distributionInterviewsMap.entrySet()) {
            List<Interval> tmpInterviews = entry.getValue();
            tmpInterviews.sort(new Comparator<Interval>() {
                @Override
                public int compare(Interval o1, Interval o2) {
                    int leftComp = Double.compare(o1.lowerEndpoint(), o2.lowerEndpoint());
                    if (leftComp != 0) {
                        return leftComp;
                    } else {
                        if (o1.lowerBoundType() == BoundType.OPEN) {
                            return 1;
                        }
                        return 0;
                    }
                }
            });

            Guard guard = Guard.create();
            int index = 0;
            while (index < tmpInterviews.size()) {
                Interval interval = tmpInterviews.get(index++);
                for (; index < tmpInterviews.size() && interval.isConnected(tmpInterviews.get(index)); index++) {
                    interval = interval.span(tmpInterviews.get(index));
                }
                guard.putInterval(interval);
            }
            distributionGuardMap.put(entry.getKey(), guard);
        }

        return distributionGuardMap;
    }

//    default Set<Transition> evidenceToPTATransition(Set<DiscreteTransition> evidences, Compatibility compatibility) {
//        Map<FastImmPair<Location, Input>, Set<DiscreteTransition>> relationMap = new HashMap<>();
//        for (DiscreteTransition evidence : evidences) {
//            FastImmPair<Location, Input> key = FastImmPair.of(evidence.getSource(), evidence.getInput());
//            relationMap.putIfAbsent(key, new HashSet<>());
//            relationMap.get(key).add(evidence);
//        }
//
//        Set<Transition> transitions = new HashSet<>();
//        for (Map.Entry<FastImmPair<Location, Input>, Set<DiscreteTransition>> group : relationMap.entrySet()) {
//            Map<Map<FastImmPair<Boolean, Location>, Integer>, Set<Double>> distributionMap = new HashMap<>();
//            Map<Double, Map<FastImmPair<Boolean, Location>, Integer>> clockValMap = new HashMap<>();
//
//            for (DiscreteTransition evidence : group.getValue()) {
//                boolean existed = false;
//                Map<FastImmPair<Boolean, Location>, Integer> distribution1 = evidence.getDistribution();
//                for (Map<FastImmPair<Boolean, Location>, Integer> distribution2 : new HashSet<>(distributionMap.keySet())) {
//                    if (compatibility.compatible(distribution1, distribution2)) {
//                        existed = true;
//                        // replace the less distribution
//                        int n1 = distribution1.values().stream().mapToInt(Integer::intValue).sum();
//                        int n2 = distribution2.values().stream().mapToInt(Integer::intValue).sum();
//                        if (n1 > n2) {
//                            Set<Double> clockValues = distributionMap.get(distribution2);
//                            distributionMap.remove(distribution2);
//                            distributionMap.put(distribution1, clockValues);
//                            for (Double clockVal : clockValues) {
//                                clockValMap.put(clockVal, distribution1);
//                            }
//                        } else {
//                            distribution1 = distribution2;
//                        }
//                        break;
//                    }
//                }
//                if (!existed) {
//                    distributionMap.put(distribution1, new HashSet<>());
//                }
//                // add the clock value
//                distributionMap.get(distribution1).add(evidence.getTime());
//                clockValMap.put(evidence.getTime(), distribution1);
//            }
//
//            // partition
//            Map<Map<FastImmPair<Boolean, Location>, Double>, Guard> distributionGuardMap = new HashMap<>();
//            List<Double> clockValList = clockValMap.keySet().stream().sorted().collect(Collectors.toList());
//            int i = 0;
//            int current = 0;
//            while (i < clockValList.size()) {
//                Map<FastImmPair<Boolean, Location>, Integer> distribution = clockValMap.get(clockValList.get(current));
//                Interval interval;
//                while (i < clockValList.size() && distribution.equals(clockValMap.get(clockValList.get(i))))
//                    i++;
//                if (i >= clockValMap.size()) {
//                    interval = Interval.create(clockValList.get(current));
//                } else {
//                    interval = Interval.create(clockValList.get(current), clockValList.get(i));
//                }
//                Map<FastImmPair<Boolean, Location>, Double> actualDistribution = new HashMap<>();
//                int n = distribution.values().stream().mapToInt(Integer::intValue).sum();
//                for (Map.Entry<FastImmPair<Boolean, Location>, Integer> edge : distribution.entrySet()) {
//                    actualDistribution.put(edge.getKey(), (double) edge.getValue() / n);
//                }
//                if (distributionGuardMap.get(actualDistribution) == null) {
//                    distributionGuardMap.put(actualDistribution, Guard.create(interval));
//                } else {
//                    distributionGuardMap.get(actualDistribution).putInterval(interval);
//                }
//                current = i;
//            }
//
//            // construct transition
//            for (Map.Entry<Map<FastImmPair<Boolean, Location>, Double>, Guard> edge : distributionGuardMap.entrySet()) {
//                for (Map.Entry<FastImmPair<Boolean, Location>, Double> entry : edge.getKey().entrySet()) {
//                    transitions.add(new Transition(group.getKey().left, group.getKey().right, edge.getValue(), entry.getValue(), entry.getKey().left, entry.getKey().right));
//                }
//            }
//        }
//
//        return transitions;
//    }
}

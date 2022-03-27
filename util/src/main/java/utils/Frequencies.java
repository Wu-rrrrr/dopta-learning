package utils;

import java.util.HashMap;
import java.util.Map;

public class Frequencies<T> {
    private Map<T, FastImmPair<Integer, Double>> actualFrequencies;

    public void put(T key, Integer frequency) {
        actualFrequencies.put(key, FastImmPair.of(frequency, null));
    }

    public Frequencies() {
        actualFrequencies = new HashMap<>();
    }

    public Frequencies(Map<T, Integer> frequencies) {
        actualFrequencies = new HashMap<>();
        for (Map.Entry<T, Integer> entry :
                frequencies.entrySet()) {
            actualFrequencies.put(entry.getKey(), FastImmPair.of(entry.getValue(), null));
        }
    }

    public Map<T, FastImmPair<Integer, Double>> getActualFrequencies() {
        return actualFrequencies;
    }

    public Map<T, Integer> getFrequencies() {
        Map<T, Integer> frequencies = new HashMap<>();
        for (Map.Entry<T, FastImmPair<Integer, Double>> entry :
                actualFrequencies.entrySet()) {
            frequencies.put(entry.getKey(), entry.getValue().left);
        }
        return frequencies;
    }

    public Map<T, Double> getActualProbability() {
        Map<T, Double> distribution = new HashMap<>();
        for (Map.Entry<T, FastImmPair<Integer, Double>> entry :
                actualFrequencies.entrySet()) {
            distribution.put(entry.getKey(), entry.getValue().right);
        }
        return distribution;
    }

    public void setActualFrequencies(Map<T, Integer> frequencies) {
        actualFrequencies = new HashMap<>();
        for (Map.Entry<T, Integer> entry :
                frequencies.entrySet()) {
            actualFrequencies.put(entry.getKey(), FastImmPair.of(entry.getValue(), null));
        }
    }

    public void setDistribution() {
        if (actualFrequencies.values().iterator().next().right == null) {
            Map<T, Integer> frequencies = getFrequencies();
            int nrObservationsOverall = frequencies.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<T, Integer> entry : frequencies.entrySet()) {
                actualFrequencies.put(entry.getKey(), FastImmPair.of(entry.getValue(), (double) entry.getValue() / nrObservationsOverall));
            }
        }
    }
}

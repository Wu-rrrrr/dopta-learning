package base.learner;

import automaton.Input;
import automaton.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import utils.FastImmPair;

import java.util.Map;

@Data
@AllArgsConstructor
public class DiscreteTransition {
    private Location source;
    private Input input;
    private double time;
    private Map<FastImmPair<Boolean, Location>, Integer> distribution;
}

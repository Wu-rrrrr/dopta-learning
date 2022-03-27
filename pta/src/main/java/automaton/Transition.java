package automaton;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Transition {
    private Location source;
    private Input input;
    private Guard guard;
    private double probability;
    private boolean reset;
    private Location target;

    private Integer frequency;

    public Transition(Location source, Input input, Guard guard, double probability, boolean reset, Location target) {
        this.source = source;
        this.input = input;
        this.guard = guard;
        this.probability = probability;
        this.reset = reset;
        this.target = target;
    }

    @Override
    public String toString() {
        return "Transition [source=" + source.getId() + ", guard=" + guard + ", input=" + input + ", probability=" + probability + ", reset="
                + reset + ", target=" + target.getId() + "]";
    }
}

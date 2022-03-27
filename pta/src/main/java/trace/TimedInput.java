package trace;

import automaton.Input;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimedInput {
    private Input input;
    private double clockVal;

    public static TimedInput create(String input, double clockVal) {
        return new TimedInput(new Input(input), clockVal);
    }

    public static TimedInput create(Input input) {
        return new TimedInput(input, 0.0);
    }

    public TimedInput convertLogicalTime(double currentLogicalTime) {
        return TimedInput.create(input.getSymbol(), currentLogicalTime + clockVal);
    }

    public String toString() {
        return String.format("(%s,%.1f)", input.toString(), clockVal);
    }
}

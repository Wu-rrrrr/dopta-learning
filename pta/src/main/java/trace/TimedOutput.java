package trace;

import automaton.Output;
import lombok.AllArgsConstructor;
import lombok.Data;
import utils.FastImmPair;

@Data
@AllArgsConstructor
public class TimedOutput {
    private boolean reset;
    private Output output;

    public static TimedOutput create(boolean reset, String output) {
        return new TimedOutput(reset, new Output(output));
    }

    public static TimedOutput create(FastImmPair<Boolean, String> output) {
        return TimedOutput.create(output.left, output.right);
    }

    public static TimedOutput createInit(String init) {
        return new TimedOutput(true, Output.create(init));
    }

    public String toString() {
        return String.format("(%b,%s)", reset, output.toString());
    }
}

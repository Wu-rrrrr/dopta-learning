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

    @Override
    public String toString() {
        return String.format("(%b,%s)", reset, output.toString());
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null)
//            return false;
////        if(hashCode() != obj.hashCode())
////            return false;
//        if (getClass() != obj.getClass())
//            return false;
//        TimedOutput other = (TimedOutput) obj;
//        if (reset ^ other.reset) {
//            return false;
//        }
//        if (output == null) {
//            return other.output == null;
//        } else return output.equals(other.output);
//    }
}

package automaton;

import lombok.AllArgsConstructor;
import lombok.Data;
import trace.TimedOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class OutputDistribution {
    private List<Boolean> resets;
    private Map<TimedOutput, Double> distribution;

    public OutputDistribution() {
        resets = new ArrayList<>();
    }

//    @Override
//    public int hashCode() {
//        if (distribution == null)
//            return -1;
//        int hash = 0;
//        for (Boolean reset :
//                resets) {
//
//        }
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null)
//            return false;
//        if (getClass() != obj.getClass())
//            return false;
//        OutputDistribution other = (OutputDistribution) obj;
//        if (resets == null ^ other.resets == null) {
//            return false;
//        }
//        if (resets != null) {
//            if (!resets.equals(other.resets)) {
//                return false;
//            }
//        }
//        if (distribution == null ^ other.distribution == null) {
//            return false;
//        }
//        if (distribution != null) {
//            return distribution.equals(other.distribution);
//        }
//
//        return true;
//    }
}

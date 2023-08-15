package observationTable;

import automaton.OutputDistribution;
import trace.TimedOutput;
import trace.TimedSuffixTrace;

import java.util.*;

public class ExactRow {
    private Map<TimedSuffixTrace, OutputDistribution> row;

    public ExactRow(Set<TimedSuffixTrace> Ecols) {
        row = new HashMap<>();
        for (TimedSuffixTrace Ecol : Ecols) {
            row.put(Ecol, new OutputDistribution());
        }
    }

    public ExactRow() {
        row = new HashMap<>();
    }

    public void put (TimedSuffixTrace continuation, OutputDistribution answer) {
        if (row == null) {
            row = new HashMap<>();
        }
        row.put(continuation, answer);
    }

    public OutputDistribution get (TimedSuffixTrace suffix) {
        if (row == null) {
            return null;
        }
        return row.get(suffix);
    }

    public Set<TimedSuffixTrace> keySet () {
        if (row == null) {
            return null;
        }
        return row.keySet();
    }

    public Collection<OutputDistribution> values () {
        if (row == null) {
            return null;
        }
        return row.values();
    }

    public Set<Map.Entry<TimedSuffixTrace,OutputDistribution>> entrySet () {
        if (row == null) {
            return null;
        }
        return row.entrySet();
    }

    public int size () {
        if (row == null) {
            return 0;
        }
        return row.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(this.row != null){
            for(Map.Entry<TimedSuffixTrace, OutputDistribution> entry : this.row.entrySet()){
                TimedSuffixTrace suffix = entry.getKey();
                OutputDistribution cell = entry.getValue();
                sb.append(suffix).append("---").append(cell).append("|");
            }
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (Map.Entry<TimedSuffixTrace, OutputDistribution> entry :
                row.entrySet()) {
            hash += entry.hashCode() * 11;
            hash += entry.getValue().getResets().hashCode() * 19;
            if (entry.getValue().getDistribution() == null) {
                return hash;
            }
            for (Map.Entry<TimedOutput, Double> edge :
                    entry.getValue().getDistribution().entrySet()) {
                hash += edge.getKey().hashCode() * 29 + edge.getValue().hashCode() * 39;
            }
        }
        return hash;
    }

//    @Override
//    public int hashCode() {
//        if (row == null)
//            return -1;
//        int hash = 0;
//        for (Map.Entry<TimedSuffixTrace, OutputDistribution> entry :
//                row.entrySet()) {
//
//        }
//        return hash;
//    }
//

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExactRow other = (ExactRow) obj;
        if ((row == null) ^ (other.row == null)) {
            return false;
        }
        if (row != null) {
            Set<TimedSuffixTrace> commonI = new HashSet<>(row.keySet());
            commonI.retainAll(other.row.keySet());
            for (TimedSuffixTrace e : commonI) {
                if (!row.get(e).equals(other.row.get(e)))
                    return false;
            }
        }
        return true;
    }

    public boolean statRowEquivalence(ExactRow shortRow) {
        for(TimedSuffixTrace Etrace : row.keySet()){
            OutputDistribution thisAnswer = row.get(Etrace);
            OutputDistribution otherCell = shortRow.get(Etrace);
            if (!thisAnswer.equals(otherCell))
                return false;
        }
        return true;
    }
}

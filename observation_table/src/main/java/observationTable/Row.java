package observationTable;

import base.Compatibility;
import base.learner.Answer;
import trace.TimedSuffixTrace;

import java.util.*;

public class Row {
    private Map<TimedSuffixTrace, Answer> row;

    public Row(Set<TimedSuffixTrace> Ecols) {
        row = new HashMap<>();
        for (TimedSuffixTrace Ecol :
                Ecols) {
            row.put(Ecol, new Answer(false, new ArrayList<>(), new HashMap<>()));
        }
    }

    public Row() {
        row = new HashMap<>();
    }

    public boolean isIncomplete(){
        for(Answer cell : row.values()){
            if(!cell.isComplete())
                return true;
        }
        return false;
    }

    public void put (TimedSuffixTrace continuation, Answer answer) {
        if (row == null) {
            row = new HashMap<>();
        }
        row.put(continuation, answer);
    }

    public Answer get (TimedSuffixTrace suffix) {
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

    public Collection<Answer> values () {
        if (row == null) {
            return null;
        }
        return row.values();
    }

    public Set<Map.Entry<TimedSuffixTrace,Answer>> entrySet () {
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
            for(Map.Entry<TimedSuffixTrace, Answer> entry : this.row.entrySet()){
                TimedSuffixTrace suffix = entry.getKey();
                Answer cell = entry.getValue();
                sb.append(suffix).append("---").append(cell).append("|");
            }
        }
        return sb.toString();
    }

    public boolean statRowEquivalence(Row shortRow, Compatibility compChecker) {
        for(TimedSuffixTrace Etrace : row.keySet()){
            Answer thisAnswer = row.get(Etrace);
            Answer otherCell = shortRow.get(Etrace);
            if(!thisAnswer.answerEqual(otherCell, compChecker))
                return false;
        }
        return true;
    }
}

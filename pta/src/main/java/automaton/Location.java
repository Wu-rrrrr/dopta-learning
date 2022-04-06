package automaton;

import lombok.Data;
import trace.ResetTimedTrace;

import java.util.*;

@Data
public class Location {
    private int id = 0;
    private Output label;
    private Map<Input, Set<Transition>> transitions = new HashMap<>();
    private ResetTimedTrace traceRep = null;
    private List<Transition> allTransitions = null;

    @Override
    public String toString() {
        return "q[" + id + "," + label + "]";
    }

    public Location(int id, Output label) {
        this.id = id;
        this.label = label;
    }
    public Location(int id, Output label, ResetTimedTrace traceRep) {
        this.id = id;
        this.label = label;
        this.traceRep = traceRep;
    }

    public Location deepCopy() {
        Location copied = new Location(id, Output.create(label.getSymbol()));
        copied.transitions = new HashMap<>();
        for (Map.Entry<Input, Set<Transition>> entry : transitions.entrySet()) {
            copied.transitions.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        if (this.traceRep != null) {
            copied.traceRep = this.traceRep;
        }
        if (this.allTransitions != null) {
            copied.allTransitions = new ArrayList<>(this.allTransitions);
        }
        return copied;
    }

    public static Location chaos(Set<Input> inputs){
        Location s = new Location(-1, Output.chaos(), null);
        for(Input input : inputs){
            s.addTransition(Guard.COMPLEMENT_GUARD, input, 1.0, true, s);
        }
        return s;
    }

    public static Location invalid(Set<Input> inputs){
        Location s = new Location(-2, Output.invalid(), null);
        for(Input input : inputs){
            s.addTransition(Guard.COMPLEMENT_GUARD, input, 1.0, true, s);
        }
        return s;
    }

    public void addTransition(Guard guard, Input input, double probability, boolean reset, Location target) {
        allTransitions = null;
        Set<Transition> transitionsForInput = transitions.computeIfAbsent(input, k -> new HashSet<>());
        Transition addedTrans = new Transition(this, input, guard, probability, reset, target);
        transitionsForInput.add(addedTrans);
    }

    public Transition addTransition(Guard guard, Input input, double probability, boolean reset, Location target, Integer frequency) {
        allTransitions = null;
        Set<Transition> transitionsForInput = transitions.computeIfAbsent(input, k -> new HashSet<>());
        Transition addedTrans = new Transition(this, input, guard, probability, reset, target, frequency);
        transitionsForInput.add(addedTrans);
        return addedTrans;
    }

    // hashCode and equals only for label and id
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Location other = (Location) obj;
        if (id != other.id)
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        return true;
    }

    /**
     * All transitions from this state with any inputs.
     *
     * @return all transitions
     */
    public List<Transition> getAllTransitions() {
        if(allTransitions != null)
            return allTransitions;
        allTransitions = new ArrayList<>();
        for(Set<Transition> transForInput : getTransitions().values())
            allTransitions.addAll(transForInput);
        return allTransitions;
    }
}

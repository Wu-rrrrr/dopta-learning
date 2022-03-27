package base;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Compatibility {
    private double alpha;
    private boolean adaptiveAlpha;
    private boolean sameSupportCheck = true;

    public Compatibility() {
        super();
        this.alpha = 0.05;
        this.adaptiveAlpha = false;
    }
    public Compatibility(double alpha, boolean adaptiveAlpha, boolean sameSupportCheck) {
        super();
        this.alpha = alpha;
        this.adaptiveAlpha = adaptiveAlpha;
        this.sameSupportCheck = sameSupportCheck;
    }

    public boolean compatible(Map<?, Integer> entries1, Map<?, Integer> entries2) {
        if(entries1.isEmpty() || entries2.isEmpty()) // need to have this
            return true;
        if(sameSupportCheck && !sameSupport(entries1,entries2))
            return false;
        Set<Object> allKeys = new HashSet<>(entries1.keySet());
        allKeys.addAll(entries2.keySet());
        int n1 = computeTotalEntries(entries1);
        int n2 = computeTotalEntries(entries2);
        double eps1 = epsilon(alpha,n1);
        double eps2 = epsilon(alpha,n2);

        for(Object key : allKeys){
            Integer entry1 = entries1.get(key);
            Integer entry2 = entries2.get(key);
            if(entry1 == null)
                entry1 = 0;
            if(entry2 == null)
                entry2 = 0;
            if(!checkSingle(entry1,entry2,n1,n2,eps1,eps2))
                return false;
        }
        return true;
    }

    private boolean sameSupport(Map<?, Integer> entries1, Map<?, Integer> entries2) {
        for(Object e1 : entries1.keySet()){
            if(!entries2.containsKey(e1))
                return false;
        }
        for(Object e2 : entries2.keySet()){
            if(!entries1.containsKey(e2))
                return false;
        }
        return true;
    }

    private boolean checkSingle(Integer entry1, Integer entry2, int n1, int n2, double eps1, double eps2) {
        return Math.abs((double)entry1/n1 - (double)entry2/n2) < eps1 + eps2;
    }

    private int computeTotalEntries(Map<?, Integer> entries) {
        int total = 0;
        for(Integer v : entries.values())
            total+= v;
        return total;
    }

    private double epsilon(double alpha,int m){
        return Math.sqrt(1.0/(2*m) * Math.log(2.0/alpha));
    }

    public String description() {
        return "hoeffding(alpha="+alpha +", same-support-check=" + sameSupportCheck + ", adaptive-alpha=" +
                adaptiveAlpha+")";
    }

    public void updateNrTests(int nrTests) {
        if(adaptiveAlpha)
            alpha = 1.0 / Math.pow(nrTests,2.1);
    }

    public boolean isAdaptive() {
        return adaptiveAlpha;
    }
}

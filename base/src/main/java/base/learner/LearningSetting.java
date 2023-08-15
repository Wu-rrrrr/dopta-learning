package base.learner;

import automaton.PTA;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import util.export.RMLExporter;

@Data
@NoArgsConstructor
public class LearningSetting {
    private RMLExporter rmlExp = null;
    private int printFrequency = 50;

    private int nrEq;
    private int rounds;
    private long time;
    private PTA hypothesis;
    private PTA exactHypothesis;

    public LearningSetting(int printFrequency, RMLExporter rmlExp) {
        this.printFrequency = printFrequency;
        this.rmlExp = rmlExp;
    }

}

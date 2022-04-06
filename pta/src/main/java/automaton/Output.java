package automaton;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class Output {

    private String symbol = null;
    private Integer hashCache = null;
    Set<String> satisfiedProps = null;

    public Output(String stringSymbol) {
        this.symbol = stringSymbol;
    }

    public static Output create(String symbol) {
        return new Output(symbol);
    }
    public static Output chaos(){
        return new Output("Chaos");
    }
    public static Output invalid(){
        return new Output("Invalid");
    }
    public static Output sink() {
        return new Output("sink");
    }

    public Set<String> getSatisfiedProps() {
        if(satisfiedProps != null)
            return satisfiedProps;
        else{
            if(!getSymbol().contains("&"))
                satisfiedProps = Collections.singleton(getSymbol());
            else
                satisfiedProps = Arrays.stream(getSymbol().split("&")).map(String::trim).collect(Collectors.toSet());
        }
        return satisfiedProps;
    }

    @Override
    public String toString() {
        return  symbol;
    }

    @Override
    public int hashCode() {
        if(hashCache != null)
            return hashCache;
        final int prime = 31;
        int result = 1;
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        hashCache = result;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if(hashCode() != obj.hashCode())
            return false;
        if (getClass() != obj.getClass())
            return false;
        Output other = (Output) obj;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        return true;
    }
}

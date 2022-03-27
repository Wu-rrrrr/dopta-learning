package automaton;

import lombok.Data;

@Data
public class Input {
    private String symbol = null;
    private Integer hashCache = null;

    @Override
    public String toString() {
        return  symbol;
    }

    public Input(String symbol) {
        this.symbol = symbol;
    }

    public static Input create(String symbol) {
        return new Input(symbol);
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
        Input other = (Input) obj;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        return true;
    }
}

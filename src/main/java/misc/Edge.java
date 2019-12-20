package misc;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Edge {
    public String src;
    public String dst;
    public int weight;

    @Override
    public String toString() {
        return String.format("%s-%s,%d", src, dst, weight);
    }

}

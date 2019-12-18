package Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sics.kompics.KompicsEvent;

@Data
@AllArgsConstructor
public class RoutingMessage implements KompicsEvent {
    public String src;
    public String dst;
    public int weight;
    public int edge_weight;
}

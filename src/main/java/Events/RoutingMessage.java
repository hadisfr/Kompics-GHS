package Events;

import se.sics.kompics.KompicsEvent;

public class RoutingMessage implements KompicsEvent {
    public String src;
    public String dst;
    public int weight;
    public int edge_weight;

    public RoutingMessage(String src, String dst, int weight, int edge_weight) {
        this.src = src;
        this.dst = dst;
        this.weight = weight;
        this.edge_weight = edge_weight;
    }
}

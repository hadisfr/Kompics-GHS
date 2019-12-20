package Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sics.kompics.KompicsEvent;

import java.util.Stack;

@Data
@AllArgsConstructor
public class ReportMessage implements KompicsEvent {
    String src;
    String dst;
    String nearestEdgeNodeName;
    int nearestEdgeNodeDistance;
    Stack<String> nearestEdgeNodePath;
}

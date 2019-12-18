package Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sics.kompics.KompicsEvent;

import java.util.List;

@Data
@AllArgsConstructor
public class ReportMessage implements KompicsEvent {
    String src;
    String dst;
    String nearestEdgeNodeName;
    int nearestEdgeNodeDistance;
    List<String> nearestEdgeNodePath;
}

package Events;


import lombok.AllArgsConstructor;
import lombok.Data;
import misc.Edge;
import se.sics.kompics.KompicsEvent;

import java.util.List;

@Data
@AllArgsConstructor
public class FinalReportMessage implements KompicsEvent {
    String src;
    String dst;
    List<Edge> edges;
}

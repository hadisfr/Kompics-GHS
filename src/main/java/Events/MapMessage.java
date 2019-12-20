package Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sics.kompics.KompicsEvent;

import java.util.List;

@Data
@AllArgsConstructor
public class MapMessage implements KompicsEvent {
    String src;
    String dst;
    List<String> lines;
}

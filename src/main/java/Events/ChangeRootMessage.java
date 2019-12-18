package Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sics.kompics.KompicsEvent;

@Data
@AllArgsConstructor
public class ChangeRootMessage implements KompicsEvent {
    String src;
    String dst;
}

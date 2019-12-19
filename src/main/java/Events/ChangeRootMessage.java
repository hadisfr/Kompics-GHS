package Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sics.kompics.KompicsEvent;

import java.util.Stack;

@Data
@AllArgsConstructor
public class ChangeRootMessage implements KompicsEvent {
    String src;
    String dst;
    String target;
    Stack<String> path;
}

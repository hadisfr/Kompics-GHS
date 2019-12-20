package Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sics.kompics.KompicsEvent;

import java.util.Map;

@Data
@AllArgsConstructor
public class ReduceMessage implements KompicsEvent {
    String src;
    String dst;
    Map<String, Integer> wordsCount;
}

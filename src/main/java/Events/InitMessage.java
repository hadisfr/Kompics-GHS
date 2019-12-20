package Events;

import Components.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import se.sics.kompics.Init;

import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class InitMessage extends Init<Node> {
    public String nodeName;
    public HashMap<String, Integer> neighbours;
}
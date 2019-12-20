package Events;

import Components.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import se.sics.kompics.Init;

import java.util.HashMap;

@Data
@AllArgsConstructor
public class InitMessage extends Init<Node> {
    public String nodeName;
    public HashMap<String, Integer> neighbours;
    public String root;
}
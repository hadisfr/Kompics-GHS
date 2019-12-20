package Components;

import Events.InitMessage;
import Events.MapMessage;
import Events.ReduceMessage;
import Ports.EdgePort;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@ToString(onlyExplicitlyIncluded = true)
public class Node extends ComponentDefinition {
    private final Logger logger = LoggerFactory.getLogger(Node.class);

    Positive<EdgePort> receivePort = positive(EdgePort.class);
    Negative<EdgePort> sendPort = negative(EdgePort.class);

    @ToString.Include
    public String nodeName;
    @ToString.Include
    public String parentName;
    @ToString.Include
    public String rootName;

    HashMap<String, Integer> neighboursWeights;

    final Handler reduceHandler = new Handler<ReduceMessage>() {
        @Override
        public void handle(ReduceMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {

            }
        }
    };

    final Handler mapHandler = new Handler<MapMessage>() {
        @Override
        public void handle(MapMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {

            }
        }
    };

    final Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
//            TODO find best root
        }
    };

    private boolean isRoot() {
        return nodeName.equalsIgnoreCase(rootName);
    }

    public Node(InitMessage initMessage) {
        nodeName = initMessage.getNodeName();
        neighboursWeights = initMessage.getNeighbours();
        parentName = null;
        rootName = initMessage.getRoot();
        logger.info("{}: hello world!", nodeName);

        subscribe(startHandler, control);
        List<Handler> handlers = Arrays.asList(reduceHandler, mapHandler);
        for (Handler handler : handlers)
            subscribe(handler, receivePort);
    }
}

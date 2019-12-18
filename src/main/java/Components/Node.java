package Components;

import Events.*;
import Ports.EdgePort;
import misc.EdgeType;
import misc.TableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


public class Node extends ComponentDefinition {
    public static final int INFINITE_DISTANCE = 9999;

    private final Logger logger = LoggerFactory.getLogger(Node.class);

    Positive<EdgePort> receivePort = positive(EdgePort.class);
    Negative<EdgePort> sendPort = negative(EdgePort.class);
    public String nodeName;
    public String parentName;
    public String rootName;
    public int dist = 10000;
    public int level;

    private int waitForReport;
    private boolean waitForTestResult;

    private String nearestEdgeNodeName;
    private int nearestEdgeNodeDistance;
    private List<String> nearestEdgeNodePath;
    private List<TestMessage> postponedMessages;

    HashMap<String, Integer> neighboursWeights;
    HashMap<String, EdgeType> neighboursType;
    ArrayList<TableRow> routeTable = new ArrayList<>();

    final Handler routingHandler = new Handler<RoutingMessage>() {
        @Override
        public void handle(RoutingMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                System.out.println(nodeName + " received message : src " + event.src + " dst " + event.getDst());
                if (dist > event.weight) {
                    dist = event.weight;
                    parentName = event.src;
                    trigger(new FinalReportMessage(nodeName, parentName, dist, routeTable), sendPort);
                    System.out.println(String.format("node %s dist is: %s", nodeName, dist));
                    System.out.println(String.format("node %s parent is: %s", nodeName, parentName));
                    for (Map.Entry<String, Integer> entry : neighboursWeights.entrySet()) {
                        if (!entry.getKey().equalsIgnoreCase(parentName)) {
                            trigger(new RoutingMessage(nodeName, entry.getKey(), dist + entry.getValue(), entry.getValue()), sendPort);
                        }
                    }
                }
            }
        }
    };


    final Handler finalReportHandler = new Handler<FinalReportMessage>() {
        @Override
        public void handle(FinalReportMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                ArrayList<TableRow> newRoute = new ArrayList<>();
                newRoute.add(new TableRow(event.src, event.src, event.dist));
                for (TableRow tr : event.route_table) {
                    tr.first_node = event.src;
                    newRoute.add(tr);
                }
                for (TableRow tr : routeTable) {
                    boolean remove = false;
                    for (TableRow t : newRoute) {
                        if (tr.dst.equals(t.dst)) {
                            remove = true;
                        }
                    }
                    if (!remove) {
                        newRoute.add(tr);
                    }
                }
                routeTable = newRoute;
                if (parentName != null)
                    trigger(new FinalReportMessage(nodeName, parentName, dist, routeTable), sendPort);
                Path path = Paths.get("src/main/java/Routes/table" + nodeName + ".txt");
                OpenOption[] options = new OpenOption[]{WRITE, CREATE};
                try {
                    Files.write(path, routeTable.toString().getBytes(), options);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    final Handler testHandler = new Handler<TestMessage>() {
        @Override
        public void handle(TestMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                log("test", event.getSrc(), event.getDst(),
                        String.format("fragment: %s, level: %s", event.getRootName(), event.getLevel()));
                handleTest(event);
            }
        }
    };

    private void handleTest(TestMessage event) {
        log("test [process]", event.getSrc(), event.getDst(),
                String.format("fragment: %s, level: %s", event.getRootName(), event.getLevel()));

        if (rootName.equalsIgnoreCase(event.getRootName())) {
            neighboursType.put(event.getSrc(), EdgeType.Rejected);
            trigger(new RejectMessage(nodeName, event.getSrc()), sendPort);
        } else if (level >= event.getLevel()) {
            trigger(new AcceptMessage(nodeName, event.getSrc()), sendPort);
        } else {
            postpone(event);
        }
    }

    private void postpone(TestMessage event) {
        log("test [postpone]", event.getSrc(), event.getDst(),
                String.format("fragment: %s, level: %s", event.getRootName(), event.getLevel()));
        postponedMessages.add(event);
    }

    final Handler initiateHandler = new Handler<InitiateMessage>() {
        @Override
        public void handle(InitiateMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst()))
                handleInitiateMessage(event);
        }
    };

    final Handler acceptHandler = new Handler<AcceptMessage>() {
        @Override
        public void handle(AcceptMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                log("accept", event.getSrc(), event.getDst());
                if (neighboursWeights.get(event.getSrc()) < nearestEdgeNodeDistance) {
                    nearestEdgeNodeDistance = neighboursWeights.get(event.getSrc());
                    nearestEdgeNodeName = event.getSrc();
                    nearestEdgeNodePath = Arrays.asList(event.getSrc());
                }
                waitForTestResult = false;
                sendReport();
            }
        }
    };

    final Handler rejectHandler = new Handler<RejectMessage>() {
        @Override
        public void handle(RejectMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                log("reject", event.getSrc(), event.getDst());
                neighboursType.put(event.getSrc(), EdgeType.Rejected);
                waitForTestResult = false;
                sendTest();
            }
        }
    };
    final Handler connectHandler = new Handler<ConnectMessage>() {
        @Override
        public void handle(ConnectMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                log("connect", event.getSrc(), event.getDst());

//                TODO Complete

                handlePostponedMessages();
            }
        }
    };
    final Handler changeRootHandler = new Handler<ChangeRootMessage>() {
        @Override
        public void handle(ChangeRootMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                log("changeroot", event.getSrc(), event.getDst());

//                TODO Complete
            }
        }
    };

    final Handler reportHandler = new Handler<ReportMessage>() {
        @Override
        public void handle(ReportMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                log("report", event.getSrc(), event.getDst(),
                        String.format("candidate node: %s, dist: %s", event.getNearestEdgeNodeName(), event.getNearestEdgeNodeDistance()));

                if (event.getNearestEdgeNodeDistance() < nearestEdgeNodeDistance) {
                    nearestEdgeNodeDistance = event.getNearestEdgeNodeDistance();
                    nearestEdgeNodeName = event.getNearestEdgeNodeName();
                    nearestEdgeNodePath = event.getNearestEdgeNodePath();
                }
                waitForReport--;
                sendReport();
            }
        }
    };

    private void handleInitiateMessage(InitiateMessage event) {
        log("initiate", event.getSrc(), event.getDst(),
                String.format("fragment: %s, level: %s", event.getRootName(), event.getLevel()));

        this.rootName = event.getRootName();
        this.level = event.getLevel();

        handlePostponedMessages();

        waitForReport = 0;
        waitForTestResult = false;

        nearestEdgeNodeName = null;
        nearestEdgeNodeDistance = INFINITE_DISTANCE;
        nearestEdgeNodePath = new ArrayList<>();

        for (Map.Entry<String, EdgeType> entry : neighboursType.entrySet()) {
            if (entry.getValue() == EdgeType.Branch && !entry.getKey().equalsIgnoreCase(parentName)) {
                waitForReport++;
                trigger(new InitiateMessage(nodeName, entry.getKey(), event.getRootName(), event.getLevel()), sendPort);
            }
        }
        sendTest();
        sendReport();
    }

    private void handlePostponedMessages() {
        List<TestMessage> previousPostponedMessages = postponedMessages;
        postponedMessages = new ArrayList<>();
        for (TestMessage testMessageEvent : previousPostponedMessages)
            handleTest(testMessageEvent);
    }

    private void sendReport() {
        if (waitForReport == 0 && !waitForTestResult) {
            if (!isRoot()) {
                nearestEdgeNodePath.add(nodeName);
                trigger(new ReportMessage(nodeName, parentName, nearestEdgeNodeName, nearestEdgeNodeDistance,
                        nearestEdgeNodePath), sendPort);
            } else {
                System.err.println(nodeName + " finds candidate " + nearestEdgeNodeName);
//                TODO Complete
            }
        }
    }

    private void sendTest() {
        int nearestNeighbourDistance = INFINITE_DISTANCE;
        String nearestNeighbour = null;
        for (Map.Entry<String, EdgeType> entry : neighboursType.entrySet()) {
            if (entry.getValue() == EdgeType.Basic)
                if (neighboursWeights.get(entry.getKey()) < nearestNeighbourDistance) {
                    nearestNeighbourDistance = neighboursWeights.get(entry.getKey());
                    nearestNeighbour = entry.getKey();
                }
        }
        if (nearestNeighbour != null) {
            waitForTestResult = true;
            trigger(new TestMessage(nodeName, nearestNeighbour, rootName, level), sendPort);
        }
    }


    final Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            handleInitiateMessage(new InitiateMessage(nodeName, nodeName, nodeName, 0));

//            if (isRoot()) {
//                dist = 0;
//                for (Map.Entry<String, Integer> entry : neighbours_weights.entrySet()) {
//                    trigger(new RoutingMessage(nodeName, entry.getKey(),
//                            dist + entry.getValue(), entry.getValue()), sendPort);
//                }
//
//            }
        }
    };

    private boolean isRoot() {
        return nodeName.equalsIgnoreCase(rootName);
    }

    private void log(String type, String src, String dst, String msg) {
        logger.info("{} ({} -> {}): {}", type, src, dst, msg);
    }

    private void log(String type, String src, String dst) {
        log(type, src, dst, "");
    }

    public Node(InitMessage initMessage) {
        nodeName = initMessage.nodeName;
        logger.info("Node {}", nodeName);
        neighboursWeights = initMessage.neighbours;
        neighboursType = new HashMap<>();
        for (Map.Entry<String, Integer> entry : neighboursWeights.entrySet()) {
            neighboursType.put(entry.getKey(), EdgeType.Basic);
        }
        parentName = nodeName;
        rootName = nodeName;

        waitForReport = 0;
        waitForTestResult = false;

        nearestEdgeNodeName = null;
        nearestEdgeNodeDistance = INFINITE_DISTANCE;
        nearestEdgeNodePath = new ArrayList<>();
        postponedMessages = new ArrayList<>();

        subscribe(startHandler, control);
        List<Handler> handlers = Arrays.asList(
                routingHandler, finalReportHandler,
                initiateHandler, testHandler, reportHandler, acceptHandler, acceptHandler, rejectHandler,
                changeRootHandler, connectHandler
        );
        for (Handler handler : handlers)
            subscribe(handler, receivePort);
    }
}

package Components;

import Events.*;
import Ports.EdgePort;
import lombok.ToString;
import misc.Edge;
import misc.EdgeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@ToString(onlyExplicitlyIncluded = true)
public class Node extends ComponentDefinition {
    public static final int INFINITE_DISTANCE = 9999;

    private final Logger logger = LoggerFactory.getLogger(Node.class);

    Positive<EdgePort> receivePort = positive(EdgePort.class);
    Negative<EdgePort> sendPort = negative(EdgePort.class);

    @ToString.Include
    public String nodeName;
    @ToString.Include
    public String parentName;
    @ToString.Include
    public String rootName;
    @ToString.Include
    public int level;

    private int waitForReport;
    private boolean waitForTestResult;

    private String nearestEdgeNodeName;
    private int nearestEdgeNodeDistance;
    private Stack<String> nearestEdgeNodePath;
    private List<TestMessage> postponedMessages;
    private Set<String> waitForMergeSent;
    private Map<String, Integer> waitForMergeReceived;
    private Set<String> waitForAbsorbReport;

    private int waitForFinalReport;
    private List<Edge> edges;

    HashMap<String, Integer> neighboursWeights;
    HashMap<String, EdgeType> neighboursType;

    final Handler finalReportRequestHandler = new Handler<FinalReportRequestMessage>() {
        @Override
        public void handle(FinalReportRequestMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                handleFinalReportRequest(event);
            }
        }
    };

    private void handleFinalReportRequest(FinalReportRequestMessage event) {
        waitForFinalReport = 0;
        for (Entry<String, EdgeType> entry : neighboursType.entrySet()) {
            if (entry.getValue() == EdgeType.Branch && !entry.getKey().equalsIgnoreCase(parentName)) {
                waitForFinalReport++;
                trigger(new FinalReportRequestMessage(nodeName, entry.getKey()), sendPort);
            }
        }
        handleFinalReport();
    }

    final Handler finalReportHandler = new Handler<FinalReportMessage>() {
        @Override
        public void handle(FinalReportMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                edges.addAll(event.getEdges());
                waitForFinalReport--;
                handleFinalReport();
            }
        }
    };

    private void handleFinalReport() {
        if (waitForFinalReport == 0) {
            for (Entry<String, EdgeType> entry : neighboursType.entrySet()) {
                if (entry.getValue() == EdgeType.Branch && !entry.getKey().equalsIgnoreCase(parentName)) {
                    edges.add(new Edge(nodeName, entry.getKey(), neighboursWeights.get(entry.getKey())));
                }
            }
            if (!isRoot()) {
                trigger(new FinalReportMessage(nodeName, parentName, edges), sendPort);
            } else {
                logger.warn("edges: {}", edges);
                writeMst(edges);
            }
        }
    }

    private void writeMst(List<Edge> edges) {
        try {
            FileWriter fileWriter = new FileWriter("src/main/resources/mst.txt");
            fileWriter.write(edges.stream().map(Edge::toString).collect(Collectors.joining("\n")) + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final Handler testHandler = new Handler<TestMessage>() {
        @Override
        public void handle(TestMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                logger.info("{} ({}): recv {}", nodeName, rootName, event);
                handleTest(event);
            }
        }
    };

    private void handleTest(TestMessage event) {
        logger.info("{} ({}): proc {}", nodeName, rootName, event);

        if (rootName.equalsIgnoreCase(event.getRootName())) {
            if (neighboursType.get(event.getSrc()) == EdgeType.Basic)
                neighboursType.put(event.getSrc(), EdgeType.Rejected);
            logger.debug("{} ({}): neighboursType={} inside {}", nodeName, rootName, neighboursType, "handle test");
            logger.debug("{} ({}): send {}", nodeName, rootName, new RejectMessage(nodeName, event.getSrc()));
            trigger(new RejectMessage(nodeName, event.getSrc()), sendPort);
        } else if (level >= event.getLevel()) {
            logger.debug("{} ({}): send {}", nodeName, rootName, new AcceptMessage(nodeName, event.getSrc()));
            trigger(new AcceptMessage(nodeName, event.getSrc()), sendPort);
        } else {
            postpone(event);
        }
    }

    private void postpone(TestMessage event) {
        logger.info("{} ({}): postpone {}", nodeName, rootName, event);
        postponedMessages.add(event);
    }

    final Handler initiateHandler = new Handler<InitiateMessage>() {
        @Override
        public void handle(InitiateMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst()))
                handleInitiateMessage(event);
        }
    };

    final Handler absrobNoticeHandler = new Handler<AbsorbNoticeMessage>() {
        @Override
        public void handle(AbsorbNoticeMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                logger.info("{} ({}): recv {}", nodeName, rootName, event);
                handleInitiateAndAbsorbNoticeMessages(event.getSrc(), event.getRootName(), event.getLevel());
                for (Entry<String, EdgeType> entry : neighboursType.entrySet()) {
                    if (entry.getValue() == EdgeType.Branch && !entry.getKey().equalsIgnoreCase(parentName)) {
                        waitForReport++;
                        logger.debug("{} ({}): waitForReport={}, waitForTestResult={}", nodeName, rootName, waitForReport, waitForTestResult);
                        logger.debug("{} ({}): neighboursType={} inside {}", nodeName, rootName, neighboursType, "handle absorb notice");
                        logger.debug("{} ({}): send {}", nodeName, rootName, new InitiateMessage(nodeName, entry.getKey(), event.getRootName(), event.getLevel()));
                        trigger(new AbsorbNoticeMessage(nodeName, entry.getKey(), event.getRootName(), event.getLevel()), sendPort);
                    }
                }
            }
        }
    };

    final Handler acceptHandler = new Handler<AcceptMessage>() {
        @Override
        public void handle(AcceptMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                logger.info("{} ({}): recv {}", nodeName, rootName, event);
                if (neighboursWeights.get(event.getSrc()) < nearestEdgeNodeDistance) {
                    nearestEdgeNodeDistance = neighboursWeights.get(event.getSrc());
                    nearestEdgeNodeName = event.getSrc();
                    nearestEdgeNodePath = new Stack<>();
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
                logger.info("{} ({}): recv {}", nodeName, rootName, event);
                if (neighboursType.get(event.getSrc()) == EdgeType.Basic)
                    neighboursType.put(event.getSrc(), EdgeType.Rejected);
                logger.debug("{} ({}): neighboursType={} inside {}", nodeName, rootName, neighboursType, "handle reject");
                waitForTestResult = false;
                sendTest();
                sendReport();
            }
        }
    };

    final Handler connectHandler = new Handler<ConnectMessage>() {
        @Override
        public void handle(ConnectMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                logger.info("{} ({}): recv {} while waitForMerge={}", nodeName, rootName, event, waitForMergeSent);

                if (event.getLevel() < level) {
                    assert !waitForMergeSent.contains(event.getSrc()) : "merge state in absorb level state (" + event.getSrc() + " in " + nodeName + ")";
                    absorb(event.getSrc());
                } else {
                    assert !waitForMergeReceived.containsValue(event.getSrc()) : "duplicate connect?";

                    if (waitForMergeSent.contains(event.getSrc())) {
                        waitForMergeSent.remove(event.getSrc());
                        merge(event.getSrc());
                    } else
                        waitForMergeReceived.put(event.getSrc(), event.getLevel());
                }
                logger.debug("{} ({}): waitForMerge={} inside {}", nodeName, rootName, waitForMergeSent, "handle connect");
            }
        }
    };

    private void handlePostponedConnects() {
        List<String> nodesToBeAbsorbed = new ArrayList<>();
        for (Iterator<Entry<String, Integer>> it = waitForMergeReceived.entrySet().iterator(); it.hasNext(); ) {
            Entry<String, Integer> entry = it.next();
            if (entry.getValue() < level) {
                assert !waitForMergeSent.contains(entry.getKey()) : "merge state in absorb level state (" + entry.getKey() + " in " + nodeName + ")";
                it.remove();
                nodesToBeAbsorbed.add(entry.getKey());
            }
        }
        nodesToBeAbsorbed.forEach(this::absorb);
    }

    private void merge(String partner) {
        logger.warn("{} ({}): merge {} with {}", nodeName, rootName, partner, nodeName);

        neighboursType.put(partner, EdgeType.Branch);
        logger.debug("{} ({}): neighboursType={} inside {}", nodeName, rootName, neighboursType, "merge");

        if (isNewRootAfterMergeWith(partner)) {
            logger.debug("{} ({}): send {}", nodeName, rootName, new InitiateMessage(nodeName, nodeName, nodeName, level + 1));
            handleInitiateMessage(new InitiateMessage(nodeName, nodeName, nodeName, level + 1));
        }

        handlePostponedMessages();
    }

    private boolean isNewRootAfterMergeWith(String requesterNode) {
        return nodeName.compareToIgnoreCase(requesterNode) < 0;
    }

    private void absorb(String partner) {
        logger.warn("{} ({}): absorb {} into {}", nodeName, rootName, partner, nodeName);

        if (neighboursType.get(partner) == EdgeType.Basic)
            neighboursType.put(partner, EdgeType.Branch);
        logger.debug("{} ({}): neighboursType={} inside {}", nodeName, rootName, neighboursType, "absorb");

        logger.debug("{} ({}): send {}", nodeName, rootName, new AbsorbNoticeMessage(nodeName, partner, rootName, level));
        trigger(new AbsorbNoticeMessage(nodeName, partner, rootName, level), sendPort);

        handlePostponedMessages();
    }

    final Handler changeRootHandler = new Handler<ChangeRootMessage>() {
        @Override
        public void handle(ChangeRootMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                handleChangeRoot(event);
            }
        }
    };

    private void handleChangeRoot(ChangeRootMessage event) {
        logger.info("{} ({}): recv {}", nodeName, rootName, event);

        if (event.getPath().size() > 0) {
            String dst = event.getPath().pop();
            trigger(new ChangeRootMessage(nodeName, dst, event.getTarget(), (Stack<String>) event.getPath().clone()), sendPort);
        } else {
            trigger(new ConnectMessage(nodeName, event.getTarget(), level), sendPort);

            assert !waitForMergeSent.contains(event.getTarget()) : "duplicate changeroot?";

            if (waitForMergeReceived.containsKey(event.getTarget())) {
                waitForMergeReceived.remove(event.getTarget());
                merge(event.getTarget());
            } else
                waitForMergeSent.add(event.getTarget());
            logger.debug("{} ({}): waitForMerge={} inside {}", nodeName, rootName, waitForMergeSent, "handle changeroot");
        }
    }

    final Handler reportHandler = new Handler<ReportMessage>() {
        @Override
        public void handle(ReportMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                logger.info("{} ({}): recv {}", nodeName, rootName, event);

                if (event.getNearestEdgeNodeDistance() < nearestEdgeNodeDistance) {
                    nearestEdgeNodeDistance = event.getNearestEdgeNodeDistance();
                    nearestEdgeNodeName = event.getNearestEdgeNodeName();
                    nearestEdgeNodePath = (Stack<String>) event.getNearestEdgeNodePath().clone();
                }
                waitForReport--;
                logger.debug("{} ({}): waitForReport={}, waitForTestResult={}", nodeName, rootName, waitForReport, waitForTestResult);
                assert waitForReport >= 0 : "negative waitForReport";
                sendReport();
            }
        }
    };

    private void handleInitiateAndAbsorbNoticeMessages(String msgSrc, String msgRootName, int msgLevel) {
        rootName = msgRootName;
        level = msgLevel;
        parentName = msgSrc;

        if (!parentName.equalsIgnoreCase(nodeName)) {
            if (neighboursType.get(parentName) == EdgeType.Basic)
                neighboursType.put(parentName, EdgeType.Branch);
            logger.debug("{} ({}): neighboursType={} inside {}", nodeName, rootName, neighboursType, "handle initiate & absorb notice");
        }

        handlePostponedMessages();
        handlePostponedConnects();

        waitForReport = 0;
        waitForTestResult = false;
        logger.debug("{} ({}): waitForReport={}, waitForTestResult={}", nodeName, rootName, waitForReport, waitForTestResult);

        waitForMergeSent.remove(msgSrc);
        waitForMergeReceived.remove(msgSrc);
        logger.debug("{} ({}): waitForMerge={} inside {}", nodeName, rootName, waitForMergeSent, "handle initiate & absorb notice");

        nearestEdgeNodeName = null;
        nearestEdgeNodeDistance = INFINITE_DISTANCE;
        nearestEdgeNodePath = new Stack<>();
    }

    private void handleInitiateMessage(InitiateMessage event) {
        logger.info("{} ({}): recv {}", nodeName, rootName, event);
        handleInitiateAndAbsorbNoticeMessages(event.getSrc(), event.getRootName(), event.getLevel());
        for (Entry<String, EdgeType> entry : neighboursType.entrySet()) {
            if (entry.getValue() == EdgeType.Branch && !entry.getKey().equalsIgnoreCase(parentName)) {
                waitForReport++;
                logger.debug("{} ({}): waitForReport={}, waitForTestResult={}", nodeName, rootName, waitForReport, waitForTestResult);
                logger.debug("{} ({}): neighboursType={} inside {}", nodeName, rootName, neighboursType, "handle initiate");
                logger.debug("{} ({}): send {}", nodeName, rootName, new InitiateMessage(nodeName, entry.getKey(), event.getRootName(), event.getLevel()));
                trigger(new InitiateMessage(nodeName, entry.getKey(), event.getRootName(), event.getLevel()), sendPort);
            }
        }
        sendTest();
        sendReport();
    }

    private void handlePostponedMessages() {
        List<TestMessage> previousPostponedMessages = (List<TestMessage>) ((ArrayList<TestMessage>) postponedMessages).clone();
        postponedMessages = new ArrayList<>();
        for (TestMessage testMessageEvent : previousPostponedMessages)
            handleTest(testMessageEvent);
    }

    private void sendReport() {
        logger.debug("{} ({}): waitForReport={}, waitForTestResult={}", nodeName, rootName, waitForReport, waitForTestResult);
        if (waitForReport == 0 && !waitForTestResult) {
            if (!isRoot()) {
                if (nearestEdgeNodeDistance < INFINITE_DISTANCE)
                    nearestEdgeNodePath.push(nodeName);
                logger.debug("{} ({}): send {}", nodeName, rootName, new ReportMessage(nodeName, parentName, nearestEdgeNodeName, nearestEdgeNodeDistance, (Stack<String>) nearestEdgeNodePath.clone()));
                trigger(new ReportMessage(nodeName, parentName, nearestEdgeNodeName, nearestEdgeNodeDistance, (Stack<String>) nearestEdgeNodePath.clone()), sendPort);
            } else {
                if (nearestEdgeNodeDistance < INFINITE_DISTANCE) {
                    String dst = nearestEdgeNodePath.size() > 0 ? nearestEdgeNodePath.peek() : nodeName;
                    handleChangeRoot(new ChangeRootMessage(nodeName, dst, nearestEdgeNodeName, (Stack<String>) nearestEdgeNodePath.clone()));
                } else {
                    terminate();
                }
            }
        }
    }

    private void terminate() {
        logger.info("{} ({}): GHS terminated", nodeName, rootName);

        handleFinalReportRequest(new FinalReportRequestMessage(nodeName, rootName));
    }

    private void sendTest() {
        int nearestNeighbourDistance = INFINITE_DISTANCE;
        String nearestNeighbour = null;
        for (Entry<String, EdgeType> entry : neighboursType.entrySet()) {
            if (entry.getValue() == EdgeType.Basic)
                if (neighboursWeights.get(entry.getKey()) < nearestNeighbourDistance) {
                    nearestNeighbourDistance = neighboursWeights.get(entry.getKey());
                    nearestNeighbour = entry.getKey();
                }
        }
        if (nearestNeighbour != null) {
            waitForTestResult = true;
            logger.debug("{} ({}): send {}", nodeName, rootName, new TestMessage(nodeName, nearestNeighbour, rootName, level));
            trigger(new TestMessage(nodeName, nearestNeighbour, rootName, level), sendPort);
        }
    }


    final Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            handleInitiateMessage(new InitiateMessage(nodeName, nodeName, nodeName, 0));
        }
    };

    private boolean isRoot() {
        return nodeName.equalsIgnoreCase(rootName);
    }

    public Node(InitMessage initMessage) {
        nodeName = initMessage.nodeName;
        logger.info("{} ({}): hello world!", nodeName, rootName);
        neighboursWeights = initMessage.neighbours;
        neighboursType = new HashMap<>();
        for (Entry<String, Integer> entry : neighboursWeights.entrySet()) {
            neighboursType.put(entry.getKey(), EdgeType.Basic);
        }
        parentName = nodeName;
        rootName = nodeName;

        waitForReport = 0;
        waitForTestResult = false;

        waitForFinalReport = 0;
        edges = new ArrayList<>();

        nearestEdgeNodeName = null;
        nearestEdgeNodeDistance = INFINITE_DISTANCE;
        nearestEdgeNodePath = new Stack<>();
        postponedMessages = new ArrayList<>();
        waitForMergeSent = new HashSet<>();
        waitForMergeReceived = new HashMap<>();

        subscribe(startHandler, control);
        List<Handler> handlers = Arrays.asList(
                finalReportRequestHandler, finalReportHandler,
                initiateHandler, testHandler, reportHandler, acceptHandler, rejectHandler, changeRootHandler, connectHandler, absrobNoticeHandler
        );
        for (Handler handler : handlers)
            subscribe(handler, receivePort);
    }
}

package Components;

import Events.InitMessage;
import Events.MapMessage;
import Events.ReduceMessage;
import Ports.EdgePort;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private Map<String, Integer> neighboursWeights;
    private int waitForMap;
    private Map<String, Integer> aggregatedWordsCount;

    final Handler reduceHandler = new Handler<ReduceMessage>() {
        @Override
        public void handle(ReduceMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                logger.info("{}: rcv {}", nodeName, event);

                aggregate(event.getWordsCount());

                waitForMap--;
                logger.debug("{}: waitForMap={}", nodeName, waitForMap);
                handleReduce();
            }
        }
    };

    private void aggregate(Map<String, Integer> wordsCount) {
        aggregatedWordsCount = Stream.concat(wordsCount.entrySet().stream(), aggregatedWordsCount.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    final Handler mapHandler = new Handler<MapMessage>() {
        @Override
        public void handle(MapMessage event) {
            if (nodeName.equalsIgnoreCase(event.getDst())) {
                parentName = event.getSrc();
                logger.info("{}: rcv {}", nodeName, event);
                handleMap(event.getLines());
            }
        }
    };

    final Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
//            TODO find best root

            if (isRoot()) {
                handleMap(readData());
            }
        }
    };

    private static List<String> readData() {
//        return Arrays.asList("kompics kompics", "ds ut", "ds kompics");

        List<String> lines = new ArrayList<>();
        File resourceFile = new File("src/main/resources/text-file.txt");
        try (Scanner scanner = new Scanner(resourceFile)) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                line = line.replaceAll("[^a-zA-Z0-9 ]", "");
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private void handleMap(List<String> lines) {
        if (hasChild()) {
            distribute(lines);
        } else {
            aggregatedWordsCount = countWords(lines);
            if (!isRoot()) {
                ReduceMessage msg = new ReduceMessage(nodeName, parentName, aggregatedWordsCount);
                logger.info("{}: snd {}", nodeName, msg);
                trigger(msg, sendPort);
            } else {
                handleReduce();
            }
        }
    }

    private void handleReduce() {
        if (waitForMap == 0) {
            if (!isRoot()) {
                ReduceMessage msg = new ReduceMessage(nodeName, parentName, aggregatedWordsCount);
                logger.info("{}: snd {}", nodeName, msg);
                trigger(msg, sendPort);
            } else {
                logger.warn("{}: total wordsCount={}", nodeName, aggregatedWordsCount);
                writeResult(aggregatedWordsCount);
            }
        }
    }

    private static void writeResult(Map<String, Integer> aggregatedWordsCount) {
        try {
            FileWriter fileWriter = new FileWriter("src/main/resources/count.txt");
            fileWriter.write(aggregatedWordsCount.entrySet().stream()
                    .map(entry -> String.format("%s:%s", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining("\n")) + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Integer> countWords(List<String> lines) {
        Map<String, Integer> wordsCount = new HashMap<>();
        for (String line : lines)
            for (String word : line.split(" "))
                wordsCount.put(word.toLowerCase(), wordsCount.getOrDefault(word.toLowerCase(), 0) + 1);
        logger.debug("{}: wordsCount={}", nodeName, wordsCount);
        return wordsCount;
    }

    private void distribute(List<String> lines) {
        int linesPerNode = (int) Math.ceil((float) lines.size() / getNumberOfChildren());
        int startIndex = 0;
        for (String neighbour : neighboursWeights.keySet())
            if (!neighbour.equalsIgnoreCase(parentName)) {
                int endIndex = startIndex + linesPerNode;
                if (endIndex >= lines.size())
                    endIndex = lines.size();
                logger.debug("{}: {}, {}", nodeName, startIndex, endIndex);
                MapMessage msg = new MapMessage(nodeName, neighbour, lines.subList(startIndex, endIndex));
                startIndex += linesPerNode;
                waitForMap++;
                logger.info("{}: snd {}", nodeName, msg);
                logger.debug("{}: waitForMap={}", nodeName, waitForMap);
                trigger(msg, sendPort);
            }
    }

    private boolean isRoot() {
        return nodeName.equalsIgnoreCase(rootName);
    }

    private int getNumberOfChildren() {
        return isRoot() ? neighboursWeights.size() : neighboursWeights.size() - 1;
    }

    private boolean hasChild() {
        return getNumberOfChildren() > 0;
    }

    public Node(InitMessage initMessage) {
        nodeName = initMessage.getNodeName();
        neighboursWeights = initMessage.getNeighbours();
        parentName = null;
        rootName = initMessage.getRoot();
        logger.info("{}: hello world!", nodeName);

        waitForMap = 0;
        aggregatedWordsCount = new HashMap<>();

        subscribe(startHandler, control);
        List<Handler> handlers = Arrays.asList(reduceHandler, mapHandler);
        for (Handler handler : handlers)
            subscribe(handler, receivePort);
    }
}

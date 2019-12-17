package Components;
import Events.InitMessage;
import Events.ReportMessage;
import Events.RoutingMessage;
import Ports.EdgePort;
import misc.TableRow;
import se.sics.kompics.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


public class Node extends ComponentDefinition {
    Positive<EdgePort> recievePort = positive(EdgePort.class);
    Negative<EdgePort> sendPort = negative(EdgePort.class);
    Boolean isRoot = false;
    public String nodeName;
    public String parentName;
    int dist = 10000;

    HashMap<String,Integer> neighbours = new HashMap<>();
    ArrayList<TableRow> route_table = new ArrayList<>();

    Handler routingHandler = new Handler<RoutingMessage>(){
        @Override
        public void handle(RoutingMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)){
                System.out.println(nodeName +  " received message : src " + event.src + " dst " + event.dst);
                if (dist > event.weight){
                    dist = event.weight;
                    parentName = event.src;
                    trigger(new ReportMessage(nodeName,parentName ,dist, route_table),sendPort);
                    System.out.println(String.format("node %s dist is: %s",nodeName,dist));
                    System.out.println(String.format("node %s parent is: %s",nodeName,parentName));
                    for( Map.Entry<String, Integer> entry : neighbours.entrySet())
                    {
                        if(!entry.getKey().equalsIgnoreCase(parentName))
                        {
                            trigger(new RoutingMessage(nodeName,entry.getKey() ,dist + entry.getValue(),entry.getValue()),sendPort);
                        }
                    }
                }
            }
        }
    };


    Handler reportHandler = new Handler<ReportMessage>() {
        @Override
        public void handle(ReportMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst))
            {
                ArrayList<TableRow> newRoute = new ArrayList<>();
                newRoute.add(new TableRow(event.src,event.src, event.dist));
                for(TableRow tr:event.route_table){
                    tr.first_node = event.src;
                    newRoute.add(tr);
                }
                for(TableRow tr:route_table){
                    boolean remove = false;
                    for(TableRow t:newRoute){
                        if(tr.dst.equals(t.dst)){
                            remove = true;
                        }
                    }
                    if(!remove){
                        newRoute.add(tr);
                    }
                }
                route_table = newRoute;
                if (parentName!=null)
                    trigger(new ReportMessage(nodeName,parentName,dist ,route_table),sendPort);
                Path path = Paths.get("src/main/java/Routes/table" + nodeName + ".txt");
                OpenOption[] options = new OpenOption[] { WRITE , CREATE};
                try {
                    Files.write(path,route_table.toString().getBytes(),options);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };



    Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            if(isRoot){
                dist = 0;
                for( Map.Entry<String, Integer> entry : neighbours.entrySet())
                {
                    trigger(new RoutingMessage(nodeName,entry.getKey() ,
                            dist + entry.getValue(),entry.getValue()),sendPort);
                }

            }
        }
    };

    public Node(InitMessage initMessage) {
        nodeName = initMessage.nodeName;
        System.out.println("initNode :" + initMessage.nodeName);
        this.neighbours = initMessage.neighbours;
        this.isRoot = initMessage.isRoot;
        subscribe(startHandler, control);
        subscribe(reportHandler,recievePort);
        subscribe(routingHandler,recievePort);
    }


}


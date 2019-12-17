package Ports;

import Events.ReportMessage;
import Events.RoutingMessage;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {{
    positive(RoutingMessage.class);
    positive(ReportMessage.class);
    negative(RoutingMessage.class);
    negative(ReportMessage.class);
}}

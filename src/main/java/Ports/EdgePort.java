package Ports;

import Events.*;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {
    {
        positive(RoutingMessage.class);
        positive(FinalReportMessage.class);
        positive(InitiateMessage.class);
        positive(TestMessage.class);
        positive(ReportMessage.class);
        positive(AcceptMessage.class);
        positive(RejectMessage.class);
        positive(ConnectMessage.class);
        positive(ChangeRootMessage.class);
        negative(RoutingMessage.class);
        negative(FinalReportMessage.class);
        negative(InitiateMessage.class);
        negative(TestMessage.class);
        negative(ReportMessage.class);
        negative(AcceptMessage.class);
        negative(RejectMessage.class);
        negative(ConnectMessage.class);
        negative(ChangeRootMessage.class);
    }}

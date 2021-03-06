package Ports;

import Events.*;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {
    {
        positive(FinalReportRequestMessage.class);
        positive(FinalReportMessage.class);
        positive(InitiateMessage.class);
        positive(TestMessage.class);
        positive(ReportMessage.class);
        positive(AcceptMessage.class);
        positive(RejectMessage.class);
        positive(ConnectMessage.class);
        positive(ChangeRootMessage.class);
        positive(AbsorbNoticeMessage.class);
        negative(FinalReportRequestMessage.class);
        negative(FinalReportMessage.class);
        negative(InitiateMessage.class);
        negative(TestMessage.class);
        negative(ReportMessage.class);
        negative(AcceptMessage.class);
        negative(RejectMessage.class);
        negative(ConnectMessage.class);
        negative(ChangeRootMessage.class);
        negative(AbsorbNoticeMessage.class);
    }
}

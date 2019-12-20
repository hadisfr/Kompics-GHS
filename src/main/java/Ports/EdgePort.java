package Ports;

import Events.MapMessage;
import Events.ReduceMessage;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {
    {
        positive(MapMessage.class);
        positive(ReduceMessage.class);
        negative(MapMessage.class);
        negative(ReduceMessage.class);
    }
}

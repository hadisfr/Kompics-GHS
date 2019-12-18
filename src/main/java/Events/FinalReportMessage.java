package Events;


import lombok.AllArgsConstructor;
import lombok.Data;
import misc.TableRow;
import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;

@Data
@AllArgsConstructor
public class FinalReportMessage implements KompicsEvent {

    public String src;
    public String dst;
    public int dist;
    public ArrayList<TableRow> route_table;
}

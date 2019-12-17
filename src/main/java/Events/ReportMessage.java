package Events;


import misc.TableRow;
import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;

public class ReportMessage implements KompicsEvent {

    public String dst;
    public String src;
    public int dist;
    public ArrayList<TableRow> route_table;

    public ReportMessage( String src, String dst ,int dist, ArrayList<TableRow> route_table) {
        this.dst = dst;
        this.src = src;
        this.dist = dist;
        this.route_table = route_table;
    }
}

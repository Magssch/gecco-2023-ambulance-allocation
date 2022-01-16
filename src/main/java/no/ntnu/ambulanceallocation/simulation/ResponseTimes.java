package no.ntnu.ambulanceallocation.simulation;

import no.ntnu.ambulanceallocation.utils.TimeSeries;
import no.ntnu.ambulanceallocation.utils.Utils;


public class ResponseTimes extends TimeSeries<Integer> {

    public double average() {
        return Utils.average(values());
    }

    public double median() {
        return Utils.median(values());
    }
}

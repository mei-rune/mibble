package net.percederberg.mibble.compiler;

/**
 * Created on 2015/11/30.
 */
public class MetricSpec {
    public String metric;
    public String implName;
    public boolean isArray;

    public MetricSpec(String metric, String implName, boolean isArray) {
        this.metric = metric;
        this.implName = implName;
        this.isArray = isArray;
    }
}

package io.quadroid.ContextSwitchMeasurement.main;

/**
 * Created by darius on 13.02.14.
 */
public class Result {
    protected long cycles;
    protected long time;

    public Result(){
        this.cycles = 0;
        this.time = 0;
    }

    protected void reset() {
        this.cycles = 0;
        this.time = 0;
    }
}

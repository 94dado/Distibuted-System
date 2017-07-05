import Simulator.Buffer;
import Simulator.Measurement;

import java.util.ArrayList;
import java.util.List;

public class BombBuffer implements Buffer<Measurement> {
    ArrayList<Measurement> measurements = new ArrayList<>();

    @Override
    public synchronized void addNewMeasurement(Measurement measurement) {
        measurements.add(measurement);
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        List<Measurement> toRet = (List<Measurement>) measurements.clone();
        measurements.clear();
        return toRet;
    }
}

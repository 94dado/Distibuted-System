import Simulator.AccelerometerSimulator;
import Simulator.Measurement;

import java.util.List;

public class BombThread extends Thread{
    BombBuffer buffer;
    AccelerometerSimulator simulator;
    Thread simulatorThread;
    int second = 1000;
    private final double alfa = 0.2;
    private final double th = 3;

    public BombThread(){
        buffer = new BombBuffer();
        simulator = new AccelerometerSimulator(buffer);
        simulatorThread = new Thread(simulator);
        simulatorThread.start();
    }

    @Override
    public void run() {
        List<Measurement> measurements;
        double avg, oldEMA = 0, EMA;
        boolean firstRun = true;
        while(!GameplayManager.getIstance().isMatchFinished()){
            //aspetto un secondo
            try{
                Thread.sleep(second);
            }catch (Exception e){
                System.err.println("Errore nello sleep del thread del sensore");
                //a questo giro lo sleep e' fallito. Riprovo, semplicemente
                continue;
            }
            measurements = buffer.readAllAndClean();
            avg = average(measurements);
            if(firstRun){
                //al primo giro non ho oldema, quindi salvo come EMA avg.
                EMA = avg;
                oldEMA = EMA;
                firstRun = false;
            }else{
                //vero algoritmo
                EMA = EMA(oldEMA, avg);
                GridColor bomb = getOutliers(EMA, oldEMA);
                //se ho generato la bomba
                if(bomb != null){
                    //inserisco la bomba nella lista
                    GameplayManager.getIstance().addBomb(bomb);
                }
                //preparo variabili per giro successivo
                oldEMA = EMA;
            }
        }
    }

    private GridColor getOutliers(double EMA, double oldEMA) {
        if(EMA - oldEMA > th){
            //genero bomba!!
            int val = (int) (EMA % 4);
            switch (val){
                case 0:
                    return GridColor.GREEN;
                case 1:
                    return GridColor.RED;
                case 2:
                    return GridColor.BLUE;
                case 3:
                    return GridColor.YELLOW;
            }
        }
        return null;
    }

    private double average(List<Measurement> measurements){
        double sum = 0;
        for(Measurement m: measurements){
            sum += m.getValue();
        }
        return sum/measurements.size();
    }

    private double EMA(double oldEMA, double avg){
        return oldEMA + (alfa * (avg - oldEMA));
    }

    public void stopBomb(){
        simulatorThread.stop();
        stop();
    }
}

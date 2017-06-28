import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class PeerRequestSender {

    //metodo per inviare una richiesta singola
    public static void sendRequest(String message, Player destination){
        try{
            //create socket and streamer
            Socket socket = new Socket(destination.getAddress(), destination.getPort());
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            //send data
            outToServer.writeBytes(message + "\n");
            outToServer.flush();
            //close socket
            outToServer.close();
        }catch (Exception e){
            System.err.println("Errore nel tentativo di comunicare con un altro client");
            System.err.println("------------------------------------------------------");
            e.printStackTrace();
        }
    }

    //invia piu' messaggi in parallelo
    public static void sendRequestToAll(ArrayList<Player> players, Player me, String message) {
        ArrayList<Sender> threads = new ArrayList<>();
        //invio il messaggio a tutti
        for (Player destination : players) {
            //tranne che a me stesso
            if(!destination.equals(me)) {
                //avvio thread che mandera' messaggio
                Sender send = new Sender(destination,message);
                threads.add(send);
                send.start();
            }
        }
        //attendo che ogni thread abbia finito
        for(Sender send: threads){
            try{
                send.join();
            }catch (Exception e){
                System.err.println("Errore nell'attesa che i thread inviassero un messaggio a più destinatari");
                System.err.println("-------------------------------------------------------------------------");
                e.printStackTrace();
            }
        }
    }

    public static PlayerCoordinate spawnPlayer(ArrayList<Player> players, Player me, int dimension){
        boolean spawned = true;
        Gson gson = new Gson();
        PlayerCoordinate coordinate;
        ArrayList<PlayerCoordinate> oldAttempts = new ArrayList<>();
        //finche' non riesco a spawnare
        do{
            //creo coordinate e le metto in un messaggio
            coordinate = spawnCoordinate(dimension, oldAttempts);
            oldAttempts.add(coordinate);
            Message toSend = new Message(MessageType.CHECK_SPAWN,gson.toJson(coordinate));
            for (Player destination : players) {
                if (!destination.equals(me)) {
                    try {
                        Socket socket = new Socket(destination.getAddress(), destination.getPort());
                        DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                        //send data
                        outToServer.writeBytes(gson.toJson(toSend) + "\n");
                        outToServer.flush();
                        //attendo risposta
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        //valuto risposta
                        String answer = reader.readLine();
                        spawned = gson.fromJson(answer, boolean.class);
                        //close socket
                        outToServer.close();
                        if (!spawned) break;
                    } catch (Exception e) {
                        System.err.println("Errore nel tentativo di comunicare coi peer per spawnare");
                        System.err.println("--------------------------------------------------------");
                        e.printStackTrace();
                    }
                }
            }
        }while(!spawned);
        return coordinate;
    }

    //create random player coordinates
    private static PlayerCoordinate spawnCoordinate(int dimension, ArrayList<PlayerCoordinate> old){
        Random generator = new Random();
        PlayerCoordinate actual = new PlayerCoordinate(generator.nextInt(dimension),generator.nextInt(dimension));
        while(old.contains(actual)) actual = new PlayerCoordinate(generator.nextInt(dimension),generator.nextInt(dimension));
        return actual;
    }
}

class Sender extends Thread{
    private Player destination;
    private String message;

    public Sender(Player destination, String message) {
        this.destination = destination;
        this.message = message;
    }

    @Override
    public void run() {
        PeerRequestSender.sendRequest(message,destination);
    }
}

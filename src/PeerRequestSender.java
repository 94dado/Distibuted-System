
//classe per l'invio di richieste agli altri client

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

class Sender extends Thread{
    private GameplayManager manager;
    private String message;
    private Player destination;

    public Sender(GameplayManager manager, Player destination) {
        this.manager = manager;
        this.message = manager.message;
        this.destination = destination;
    }

    @Override
    public void run() {
        PeerRequestSender.sendRequest(manager,message,destination);
    }
}


public class PeerRequestSender {

    //metodo per inviare una richiesta singola
    public static void sendRequest(GameplayManager manager, String message, Player destination){
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
    public static void sendRequestToAll(GameplayManager manager) {
        ArrayList<Player> players = manager.getMatch().getPlayers();
        ArrayList<Sender> threads = new ArrayList<>();
        for (Player destination : players) {
            //invio a tutti tranne che a me stesso
            if(!destination.equals(manager.getMe())){
                Sender send = new Sender(manager,destination);
                threads.add(send);
                send.start();
            }
        }
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
}

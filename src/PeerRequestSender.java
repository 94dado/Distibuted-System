import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

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

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by dado_ on 04/07/2017.
 */
public class ThrowBombThread extends Thread {
    private Message message;
    private final int seconds = 5000;

    public ThrowBombThread(Message message){
        this.message = message;
    }

    @Override
    public void run() {
        Gson gson = new Gson();
        boolean ok = true;
        //avviso i player della bomba
        ArrayList<Socket> sockets = PeerRequestSender.spawnBomb(GameplayManager.getIstance().getPlayersList(), gson.toJson(message));
        for(Socket socket:sockets){
            try{
                //attendo risposta
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //valuto risposta
                Message answer = gson.fromJson(reader.readLine(),Message.class);
                socket.close();
                ok = ok && answer.getType() == MessageType.ACK;
            }catch (Exception e){
                System.err.println("Errore nello spawn della bomba");
                System.err.println("------------------------------");
                e.printStackTrace();
            }
        }
        if(!ok){
            System.err.println("Non ho ricevuto ack da tutti per lo spawn della bomba... :/");
        }
        //ora tutti sanno che la bomba e' stata spawnata. risveglio thread del token
        synchronized (GameplayManager.getIstance()){
            GameplayManager.getIstance().notify();
        }
        //attendo 5 secondi per l'esplosione
        try{Thread.sleep(seconds);}catch (Exception e){
            System.err.println("Errore nella sleep della bomba");
            System.err.println("------------------------------");
            e.printStackTrace();
        }
        //ora posso far esplodere la bomba
        message = new Message(MessageType.BOMB_EXPLODED, message.getJsonMessage());
        int killed = PeerRequestSender.sendBomb(GameplayManager.getIstance().getPlayersList(), gson.toJson(message));
    }
}

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class MessageHandler extends Thread{
    private Socket socket;

    public MessageHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        //elaboro messaggio
        Gson gson = new Gson();
        try{
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String messageString = inFromClient.readLine();
            //var per lo switch
            PlayerCoordinate coordinate;
            //recupero il messaggio
            Message message = gson.fromJson(messageString,Message.class);
            switch (message.getType()){
                case TOKEN:                     //ho ricevuto il token
                    GameplayManager.getIstance().tokenReceived();
                    socket.close();
                    break;
                case ADD_PLAYER:                //nuovo player in partita
                    Player player = gson.fromJson(message.getJsonMessage(),Player.class);
                    GameplayManager.getIstance().addNewPlayer(player);
                    socket.close();
                    break;
                case CHECK_SPAWN:               //richiesto check delle coordinate per lo spawn
                    coordinate = gson.fromJson(message.getJsonMessage(), PlayerCoordinate.class);
                    boolean result = GameplayManager.getIstance().checkSpawnCoordinate(coordinate);
                    PeerRequestSender.answer(socket,gson.toJson(result));
                    break;
                case MOVE:                      //ricevute nuove coordinate di un player
                    coordinate = gson.fromJson(message.getJsonMessage(), PlayerCoordinate.class);
                    Message m;
                    if(GameplayManager.getIstance().checkDie(coordinate)){
                        //sono morto
                        GameplayManager.getIstance().addEvent("Sei stato mangiato. Hai perso");
                        m = new Message(MessageType.KILL_CONFIRMED, null);
                    }else{
                        m = new Message(MessageType.ACK, null);
                    }
                    PeerRequestSender.answer(socket,gson.toJson(m));
                    //se sono morto, solo ora che tutti lo sanno e non mi hanno piu' nella lista dei player, mi invio il messaggio di morte
                    if(m.getType() == MessageType.KILL_CONFIRMED){
                        GameplayManager.getIstance().sendDieMessage();
                    }
                    break;
                case DIE:                       //sono morto. concludo la mia esistenza
                    socket.close();
                    GameplayManager.getIstance().isMatchFinished(true);
                    break;
                case REMOVE_PLAYER:             //player morto. da togliere dalla lista
                    player = gson.fromJson(message.getJsonMessage(),Player.class);
                    //salvo avvenimento morte player per la stampa su output
                    GameplayManager.getIstance().addEvent(player.getName() + " è stato mangiato");
                    //rimuovo il player dalla lista
                    GameplayManager.getIstance().removePlayer(player);
                    Message answer = new Message(MessageType.ACK,null);
                    PeerRequestSender.answer(socket,gson.toJson(answer));
            }
        }catch (Exception e){
            System.err.println("Errore nell'elaborazione del messaggio ricevuto");
            System.err.println("-----------------------------------------------");
            e.printStackTrace();
        }
    }
}
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
            //recupero il messaggio
            Message message = gson.fromJson(messageString,Message.class);
            switch (message.getType()){
                case TOKEN:   //ho ricevuto il token
                    GameplayManager.getIstance().tokenReceived();
                    break;
                case ADD_PLAYER:   //nuovo player in partita
                    Player player = gson.fromJson(message.getJsonMessage(),Player.class);
                    GameplayManager.getIstance().addNewPlayer(player);
                    break;
                case CHECK_SPAWN:   //richiesto check delle coordinate per lo spawn
                    PlayerCoordinate coordinate = gson.fromJson(message.getJsonMessage(), PlayerCoordinate.class);
                    boolean result = GameplayManager.getIstance().checkSpawnCoordinate(coordinate);
                    DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                    outToServer.writeBytes(gson.toJson(result)+"\n");
                    outToServer.flush();
                    outToServer.close();
            }
        }catch (Exception e){
            System.err.println("Errore nell'elaborazione del messaggio ricevuto");
            System.err.println("-----------------------------------------------");
            e.printStackTrace();
        }
    }
}
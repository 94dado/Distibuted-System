import com.google.gson.Gson;

import java.net.ServerSocket;
import java.util.ArrayList;

public class GameplayManager {
    //singleton
    private static GameplayManager istance;

    //info of game
    private ServerSocket serverSocket;
    private Match match;
    private Player me;
    private Gson gson;
    private PlayerCoordinate myPosition;
    private boolean isMatchFinished;
    private Message messageToSend;


    //threads
    private ClientListenerThread listenerThread;

    //messages
    static final String tokenMessage = "token";
    static final String newPlayerMessage = "newPlayer";

    private GameplayManager(ServerSocket serverSocket, Match match, Player me) {
        this.serverSocket = serverSocket;
        this.match = match;
        this.me = me;
        gson = new Gson();
    }

    synchronized boolean isMatchFinished(){
        return isMatchFinished;
    }

    static synchronized GameplayManager getIstance() {
        return istance;
    }

    synchronized ServerSocket getServerSocket() {
        return serverSocket;
    }

    static synchronized void setIstance(ServerSocket serverSocket, Match match, Player me) {
        GameplayManager.istance = new GameplayManager(serverSocket, match, me);
    }

    //restituisce il prossimo player nella lista
    private Player nextPlayer(){
        int position = 0;
        ArrayList<Player> players = match.getPlayers();
        for (int i = 0; i< players.size(); i++){
            if(players.get(i).equals(me)){
                position = i;
                break;
            }
        }
        //prendo posizione successiva
        position = (position + 1) % players.size();
        return players.get(position);
    }

    //metodo che si occupa di entrare in una partita gia' creata
    synchronized void enterTheMatch(){
        startTheMatch();
        //comunico agli altri player il mio ingresso in partita
        String meJson = gson.toJson(me);
        Message m = new Message(MessageType.ADD_PLAYER,meJson);
        //invio messaggio agli altri
        PeerRequestSender.sendRequestToAll(match.getPlayers(),me,gson.toJson(m));
        //TODO aggiungere altra roba forse?
    }

    //metodo che avvia una nuova partita
    synchronized void startTheMatch(){
        //avvio thread per ascoltare i messaggi in entrata e attendo che sia pronto
        listenerThread = new ClientListenerThread();
        listenerThread.start();
        //TODO aggiungere altra roba forse?
    }

    //metodo che aggiunge un nuovo player in partita
    public synchronized void addNewPlayer(Player newPlayer){
        match.getPlayers().add(newPlayer);
        if(match.getPlayers().size() == 2){
            //posso avviare la partita ora!
            tokenReceived();
        }
    }

    //metodo che rimuove un player in partita
    public synchronized void removePlayer(Player oldPlayer){
        ArrayList<Player> players = match.getPlayers();
        int pos = -1;
        for(int i = 0; i < players.size(); i++){
            if(players.get(i).equals(oldPlayer)){
                //trovato
                pos = i;
                break;
            }
        }
        if(pos > -1) players.remove(pos);
    }


    //metodo che si occupa di far girare il token
    public synchronized void tokenReceived() {
        System.out.println("I GOT THE POWER!");
        //todo tempo code
        try{Thread.sleep(3000);}catch (Exception e){
            System.err.println("la vita fa schifo");
        }
        if(messageToSend != null){
            //ho una mossa da mandare agli altri
            if(messageToSend.getType() == MessageType.BOMB){
                //todo chiamare metodo per inviare bomba
            }else{
                //todo chiamare metodo per inviare movimento
            }
        }
        //ora mando al prossimo il token, se esiste un prossimo
        if(match.getPlayers().size() > 1){
            //c'e' qualcun'altro. Lo trovo
            Player next = nextPlayer();
            //creo il messaggio col token
            Message tokenMessage = new Message(MessageType.TOKEN,null);
            //e glielo mando
            PeerRequestSender.sendRequest(gson.toJson(tokenMessage),next);

        }
    }
}

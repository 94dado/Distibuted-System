import com.google.gson.Gson;

import javax.ws.rs.core.MediaType;
import java.net.ServerSocket;
import java.util.ArrayList;

public class GameplayManager {
    //singleton
    private static GameplayManager istance;

    //informazioni sulla partita
    private String url;                                                     //url del server
    private ServerSocket serverSocket;                                      //socket su cui ricevo richieste
    private Match match;                                                    //informazioni sul match attuale
    private Player me;                                                      //informazioni sul mio giocatore
    private Gson gson = new Gson();
    private PlayerCoordinate myPosition;                                    //coordinate del giocatore
    private ArrayList<GridColor> bombList = new ArrayList<>();             //lista delle bombe
    private boolean isMatchFinished = false;                                //booleano che indica se la partita è finita per me
    private Message messageToSend;                                          //messaggio da inviare al ricevimento del token
    private int points;                                                     //punteggio del player
    private ArrayList<String> eventBuffer = new ArrayList<>();              //buffer degli eventi da stampare e mostrare all'utente
    private boolean win = false;


    //threads
    private ClientListenerThread listenerThread;
    private ClientInOutThread inOutThread;
    private BombThread bombThread;

    private GameplayManager(ServerSocket serverSocket, Match match, Player me, String url) {
        this.serverSocket = serverSocket;
        this.match = match;
        this.me = me;
        this.url = url;
    }

    synchronized boolean isMatchFinished(){
        return isMatchFinished;
    }

    static synchronized void setIstance(ServerSocket serverSocket, Match match, Player me, String url) {
        GameplayManager.istance = new GameplayManager(serverSocket, match, me, url);
    }

    static synchronized GameplayManager getIstance() {
        return istance;
    }

    public synchronized ServerSocket getServerSocket() {
        return serverSocket;
    }

    public synchronized PlayerCoordinate getMyPosition() {
        return myPosition;
    }

    public synchronized ArrayList<GridColor> getBombList() {
        return bombList;
    }

    public synchronized Player getMe() {
        return me;
    }

    public synchronized int getPoints() {
        return points;
    }

    public synchronized ArrayList<Player> getPlayersList(){
        return match.getPlayers();
    }

    public synchronized int getLimitPoints() {
        return match.getPointLimit();
    }

    public synchronized void setMessageToSend(Message messageToSend) {
        this.messageToSend = messageToSend;
    }

    public synchronized void setMyPosition(PlayerCoordinate myPosition) {
        this.myPosition = myPosition;
    }

    public synchronized boolean messageAvailable(){
        return messageToSend != null;
    }

    //aggiungo evento all'eventbuffer
    public synchronized void addEvent(String event){
        eventBuffer.add(event);
    }

    //aggiungo bomba al buffer delle bombe
    public synchronized void addBomb(GridColor bomb){
        bombList.add(bomb);
    }

    public synchronized void addPoints(int points){
        this.points += points;
        //controllo se la partita e' finita
        if (this.points >= match.getPointLimit()) {
            //ho vinto
            win = true;
            serverDie();
            eventBuffer.add("Complimenti, hai vinto la partita!");
            //invio il messaggio per morire
            sendDieMessage();
        }
    }

    public synchronized String[] getAllEvents(){
        String[] events = new String[eventBuffer.size()];
        events = eventBuffer.toArray(events);
        eventBuffer.clear();
        return events;
    }

    //restituisce il prossimo player nella lista
    private synchronized Player nextPlayer(){
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
    public synchronized void enterTheMatch(){
        //avvio thread per ascoltare i messaggi in entrata e attendo che sia pronto
        listenerThread = new ClientListenerThread();
        listenerThread.start();
        //creo thread per input/output
        inOutThread = new ClientInOutThread();
        //creo e avvio thread per le bombe
        bombThread = new BombThread();
        bombThread.start();
        //comunico agli altri player il mio ingresso in partita
        String meJson = gson.toJson(me);
        Message m = new Message(MessageType.ADD_PLAYER,meJson);
        PeerRequestSender.addNewPlayer(match.getPlayers(),me,gson.toJson(m));
        //cerco di far spawnare il player
        myPosition = PeerRequestSender.spawnPlayer(match.getPlayers(),me,match.getDimension());
        //avvio thread per input e output
        inOutThread.start();
    }

    //metodo che avvia una nuova partita
    public synchronized void startTheMatch(){
        //avvio thread per ascoltare i messaggi in entrata e attendo che sia pronto
        listenerThread = new ClientListenerThread();
        listenerThread.start();
        //creo thread per input/output
        inOutThread = new ClientInOutThread();
        //creo e avvio thread per le bombe
        bombThread = new BombThread();
        bombThread.start();
        //spawno il player
        myPosition = PeerRequestSender.spawnPlayer(match.getPlayers(),me,match.getDimension());
        //avvio thread per input e output
        inOutThread.start();
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
        //System.out.println("I GOT THE POWER!");
        if(messageToSend != null){
            //ho una mossa da mandare agli altri
            if(messageToSend.getType() == MessageType.BOMB_SPAWNED){
                ThrowBombThread throwBomb = new ThrowBombThread(messageToSend);
                throwBomb.start();
                //attendo che il thread abbia avvisato della bomba gli altri
                try{wait();}catch (Exception e){
                    System.err.println("Errore nel wait per la bomba");
                    System.err.println("----------------------------");
                    e.printStackTrace();
                }
                //mi sono svegliato. proseguo
            }else {
                int killed = PeerRequestSender.sendMove(match.getPlayers(), me, gson.toJson(messageToSend));
                addPoints(killed);
            }
        }
        messageToSend = null;
        //sveglio il thread dell'input
        synchronized (inOutThread){
            inOutThread.notify();
        }
        //ora mando al prossimo il token, se esiste un prossimo e se non ho vinto
        if(!win && match.getPlayers().size() > 1){
            //c'e' qualcun'altro. Lo trovo
            Player next = nextPlayer();
            //creo il messaggio col token
            Message tokenMessage = new Message(MessageType.TOKEN,null);
            //e glielo mando
            PeerRequestSender.sendToken(next,gson.toJson(tokenMessage));
        }
    }

    //metodo che controlla se le coordinate ricevute sono disponibili per uno spawn
    public synchronized boolean checkSpawnCoordinate(PlayerCoordinate coordinate) {
        return !myPosition.equals(coordinate);
    }

    //metodo che dice se si hanno bombe da poter lanciare
    public synchronized boolean bombAvailable() {
        return bombList.size() > 0;
    }

    //metodo che restituisce la bomba disponibile
    public synchronized String getBombString(){
        if(bombAvailable()){
            return bombList.get(0).toString();
        }else return null;
    }

    //metodo che rimuove una bomba dalla coda e la restituisce
    public synchronized GridColor useBomb(){
        if (bombAvailable())
            return bombList.remove(0);
        else return null;
    }

    //metodo per controllare se un movimento e' lecito
    public synchronized PlayerCoordinate canMove(String choice){
        int x,y;
        switch (choice){
            case "W":
                x = myPosition.getX();
                y = myPosition.getY() - 1;
                break;
            case "A":
                x = myPosition.getX() - 1;
                y = myPosition.getY();
                break;
            case "S":
                x = myPosition.getX();
                y = myPosition.getY() + 1;
                break;
            case "D":
                x = myPosition.getX() + 1;
                y = myPosition.getY();
                break;
            default:    //metto coordinate non valide
                x = -1;
                y = -1;
        }
        return new PlayerCoordinate(x,y);
    }

    //restituisce dimensione della griglia del match
    public synchronized int getMatchDimension() {
        return match.getDimension();
    }

    //restituisce il colore della griglia della posizione attuale
    public synchronized GridColor getActualGridColor() {
        return match.getColorOfPosition(myPosition);
    }

    //controlla se sono morto per via dell'esplosione di una bomba
    public synchronized boolean checkBombDie(GridColor bomb){
        if(bomb == getActualGridColor()){
            //sono morto
            serverDie();
            return true;
        }else{
            //sono ancora vivo
            return false;
        }
    }

    //metodo per controllare se sono morto per via di una movimento avversario
    public synchronized boolean checkDie(PlayerCoordinate coordinate) {
        if(myPosition.equals(coordinate)){
            //sono morto
            serverDie();
            return true;
        }else{
            //sono ancora vivo
            return false;

        }
    }

    //metodo che esegue la morte
    public synchronized void serverDie(){
        try {
            String uri = url + "removePlayerFromMatch/" + match.getName() +"/"+me.getName();
            HTTPRequestCreator req = new HTTPRequestCreator("DELETE", MediaType.APPLICATION_JSON, uri);
            String answer = req.getAnswer();
            if(!gson.fromJson(answer,boolean.class)){
                throw new Exception("il server non mi ha rimosso dalla lista dei player del match");
            }
        }catch (Exception e){
            System.err.println("Errore nel tentativo di comunicare la mia morte al server");
            System.err.println("---------------------------------------------------------");
            e.printStackTrace();
        }
    }

    //metodo che invia un ultima richiesta di "morte" a me stesso, per esser sicuro prima di
    //elaborare ogni messaggio che ho ricevuto nel frattempo
    public synchronized void sendDieMessage() {
        //invio richiesta a me stesso per esser sicuro di andare a terminare il server
        String message = gson.toJson(new Message(MessageType.DIE,null));
        PeerRequestSender.suicide(message, me);
    }

    //metodo che conclude la partita
    public synchronized void setupEndOfMatch() {
        //la partita deve concludersi. termino thread esistenti
        isMatchFinished = true;
        inOutThread.stopInOut();
        listenerThread.stopListener();
        bombThread.stopBomb();
        //termino finalmente la mia esistenza
        System.exit(0);
    }
}

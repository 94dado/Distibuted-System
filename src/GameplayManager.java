import java.net.ServerSocket;

public class GameplayManager {
    //info of game
    private ServerSocket serverSocket;
    private Match match;
    private Player me;
    private PlayerCoordinate myPosition;
    private boolean isMatchFinished;
    private boolean founder;

    public boolean haveToken = false;
    public String message;

    //objects for locks
    public final Lock tokenLock = new Lock();
    public final Lock startLock = new Lock();
    //threads

    public ClientListenerThread listenerThread;

    //messages
    public static final String tokenMessage = "token";
    public static final String newPlayerMessage = "newPlayer";

    public GameplayManager(ServerSocket serverSocket, Match match, Player me) {
        this.serverSocket = serverSocket;
        this.match = match;
        this.me = me;
        founder = me.equals(match.getPlayers().get(0));
    }

    public synchronized boolean isMatchFinished() {
        return isMatchFinished;
    }

    public synchronized void isMatchFinished(boolean value){
        isMatchFinished = value;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public Match getMatch() {
        return match;
    }

    public Player getMe() {
        return me;
    }

    public boolean isFounder() {
        return founder;
    }

    private void waitForThread(){
        synchronized (startLock){
            try{
                startLock.wait();
            }catch (Exception e){
                System.err.println("Errore nei monitor. Uccidetemi");
                System.err.println("------------------------------");
                e.printStackTrace();
            }
        }
    }

    //metodo che si occupa di avviare localmente la partita
    public synchronized void startTheMatch(){
        //avvio thread per ascoltare i messaggi in entrata e attendo che sia pronto
        listenerThread = new ClientListenerThread(this);
        listenerThread.start();
        waitForThread();

        //se ho creato la partita, inizializzo il token
        if(founder){
            haveToken = true;
            TokenRingThread thread = new TokenRingThread(this);
            thread.start();
        }
        else{
            //se non ho creato io la partita, comunico agli altri il mio ingresso
            synchronized (match.getPlayers()){  //mi sincronizzo sulla lista dei player
                message = newPlayerMessage + " " + me.toSendableString();
                PeerRequestSender.sendRequestToAll(this);
                System.out.println("Inviato richiesta!!");
            }
        }

        //TODO iniziare il vero e proprio match
    }
}

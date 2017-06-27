import java.util.ArrayList;


public class TokenRingThread extends Thread{
    private GameplayManager manager;

    public TokenRingThread(GameplayManager manager) {
        this.manager = manager;
    }

    @Override
    public void run(){
        try{
            if(manager.message != null){
                //ho qualcosa da inviare.
                PeerRequestSender.sendRequestToAll(manager);
                //messaggio inviato. svuoto la mossa in memoria
                manager.message = null;
            }
            //ho finito. mando il token al prossimo
            Player next;
            synchronized (manager.getMatch().getPlayers()){ //mi sincronizzo sulla lista dei player per ottenere il destinatario del token
                next = nextPlayer();
            }
            synchronized (manager){
                PeerRequestSender.sendRequest(manager,GameplayManager.tokenMessage,next);
                manager.haveToken = false;
            }
        }catch (Exception e){
            System.err.println("Errore nella rete a token ring");
            System.err.println("------------------------------");
            e.printStackTrace();
        }
    }

    private Player nextPlayer(){
        int position = 0;
        ArrayList<Player> players = manager.getMatch().getPlayers();
        for (int i = 0; i< players.size(); i++){
            if(players.get(i).equals(manager.getMe())){
                position = i;
                break;
            }
        }
        position = (position + 1) % players.size();
        return players.get(position);
    }
}

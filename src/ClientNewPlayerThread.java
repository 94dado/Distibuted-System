
public class ClientNewPlayerThread extends  Thread{
    private GameplayManager manager;
    private String sendableStringOfPlayer;

    public ClientNewPlayerThread(GameplayManager manager, String sendableStringOfPlayer) {
        this.manager = manager;
        this.sendableStringOfPlayer = sendableStringOfPlayer;
    }

    @Override
    public void run() {
        Player newPlayer = new Player(sendableStringOfPlayer);
        synchronized (manager.getMatch().getPlayers()){ //mi sincronizzo sulla lista dei player
            manager.getMatch().getPlayers().add(newPlayer);
        }
    }
}

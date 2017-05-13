import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
public class Match {
    private String name;
    private int dimension;
    private int pointLimit;

    private ArrayList<Player> players;

    public Match(String name, int dimension, int pointLimit, Player founder) {
        this.name = name;
        this.dimension = dimension;
        this.pointLimit = pointLimit;

        players = new ArrayList<>();
        players.add(founder);
    }

    //metodo per aggiungere un nuovo player alla partita
    public boolean addPlayer(Player player){
        if(players.size() < dimension * dimension){
            boolean ok = true;
            for(Player pl: players){
                if(pl.getName().equals(player.getName())){
                    ok = false;
                    break;
                }
            }
            if (ok) players.add(player);
            return ok;
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public void setPointLimit(int pointLimit) {
        this.pointLimit = pointLimit;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public void setPlayers(ArrayList<Player> players) {
        this.players = players;
    }

    public int getPointLimit() {
        return pointLimit;
    }

    //metodo per rimuovere un player dalla partita
    public boolean removePlayer(String playerName) {
        Player player = null;
        for(Player pl:players){
            if(pl.getName().equals(playerName)){
                player = pl;
                break;
            }
        }
        return player!= null && players.remove(player);
    }

    public String toReadableString(){
        StringBuilder s;
        s = new StringBuilder("Nome match: " + name + "\n");
        s.append("Dimensione griglia: ").append(dimension).append("\n");
        s.append("Punti necessari alla vittoria: ").append(pointLimit).append("\n");
        s.append("Giocatori connessi:\n");
        for(Player p :players){
            s.append("- ").append(p.getName()).append("\n");
        }
        return s.toString();
    }
}

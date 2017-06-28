import java.util.ArrayList;


public class GameServerManager {
    private ArrayList<Match> matches;
    private static GameServerManager instance;

    public GameServerManager(){
        this.matches = new ArrayList<Match>();
    }

    //singleton
    public static synchronized GameServerManager getInstance(){
        if(instance == null) instance = new GameServerManager();
        return instance;
    }

    //metodo per aggiungere un nuovo match
    public synchronized boolean addMatch(Match newMatch){
        boolean ok = true;
        for(Match match:matches){
            if(match.getName().equals(newMatch.getName())){
                ok = false;
                break;
            }
        }
        if(ok) matches.add(newMatch);
        return ok;
    }

    //metodo per aggiungere un player ad un match esistente
    public synchronized Match addPlayerToMatch(Player player, String matchName) {
        Match selectedMatch = null;
        for (Match m : matches) {
            if (m.getName().equals(matchName)) {
                selectedMatch = m;
                break;
            }
        }
        boolean ok = selectedMatch!=null && selectedMatch.addPlayer(player);
        if(ok) return selectedMatch;
        else return null;
    }

    //metodo per rimuovere un giocatore da un match
    public synchronized boolean removePlayerFromMatch(String playerName, String matchName){
        Match selectedMatch = null;
        for(Match match:matches){
            if(match.getName().equals(matchName)){
                selectedMatch = match;
                break;
            }
        }
        boolean ok = selectedMatch!=null && selectedMatch.removePlayer(playerName);
        if(ok){
            if (selectedMatch.getPlayers().size() == 0){
                //cancello match perché senza giocatori
                matches.remove(selectedMatch);
            }
            //avviso altri player del giocatore uscito
            //TODO AVVISARE GLI ALTRI!!!
        }
        return ok;
    }

    //metodo per cancellare un match
    public synchronized boolean removeMatch(String matchName){
        Match selectedMatch = null;
        for(Match match:matches){
            if(match.getName().equals(matchName)){
                selectedMatch = match;
                break;
            }
        }
        return selectedMatch != null && matches.remove(selectedMatch);
    }

    //metodo per restituire i nomi di tutti i match presenti
    public synchronized ArrayList<String> getMatches(){
        ArrayList<String> toRet = new ArrayList<>();
        for(Match m: matches){
            toRet.add(m.getName());
        }
        return toRet;
    }

    //metodo per ottenere i dettagli di un match
    public synchronized Match getMatchDetail(String matchName){
        Match match = null;
        for(Match m:matches){
            if(m.getName().equals(matchName)){
                match = m;
                break;
            }
        }
        return match;
    }
}

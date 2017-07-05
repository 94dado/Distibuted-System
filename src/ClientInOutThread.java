import com.google.gson.Gson;

import java.util.Scanner;

public class ClientInOutThread extends Thread{
    Scanner s = new Scanner(System.in);
    GameplayManager manager = GameplayManager.getIstance();
    Gson gson = new Gson();

    @Override
    public void run() {
        System.out.println("Partita iniziata\n");
        while(!manager.isMatchFinished()){
            if(manager.messageAvailable()){
                //attendo che il messaggio venga inviato
                try{
                    synchronized (this){
                        wait();
                    }
                }catch (Exception e){
                    System.err.println("Errore nella wait dell'input/output");
                    //questo e' un problemone. Termino la mia esistenza
                    GameplayManager.getIstance().serverDie();
                    GameplayManager.getIstance().sendDieMessage();
                }
            }
            //stampo tutti gli eventi avvenuti
            String[] events = manager.getAllEvents();
            for(String event:events){
                System.out.println(event);
            }
            //se il match e' finito, chiudo il thread
            if(manager.isMatchFinished())  break;
            System.out.println("Punteggio: " + manager.getPoints() + "/" + manager.getLimitPoints());
            System.out.println("Ti trovi nell'area di colore " + manager.getActualGridColor()+" e in posizione " + manager.getMyPosition()+".");
            //se ci sono bombe, avviso l'utente
            if(manager.bombAvailable()) {
                System.out.println("Hai a disposizione una bomba di colore " + manager.getBombString());
            }
            printChoices();
        }
    }

    public void stopInOut(){
        //stampo tutti gli eventi avvenuti prima di morire
        String[] events = manager.getAllEvents();
        for(String event:events){
            System.out.println(event);
        }
        this.stop();
    }

    private void printChoices(){
        System.out.println("Inserisci operazione da voler eseguire");
        System.out.println("(M)uoviti");
        if(manager.bombAvailable()) System.out.println("(B)ombarda");
        String choice = s.nextLine();
        //variabili per lo switch
        Message message;
        switch (choice.toUpperCase()){
            case "M":   //devo eseguire movimento
                System.out.println("Dove ci si vuole spostare?");
                System.out.println("Inserire direzione utilizzando una lettera tra W A S D");
                choice = s.nextLine();
                PlayerCoordinate coord = manager.canMove(choice.toUpperCase());
                if(coord.isValidCoordinate(manager.getMatchDimension())){
                    //movimento possibile. Creo messaggio
                    message = new Message(MessageType.MOVE,gson.toJson(coord));
                    //e lo preparo all'invio
                    manager.setMessageToSend(message);
                    //aggiorno mie coordinate
                    manager.setMyPosition(coord);
                }else{
                    //movimento non disponibile. richiedo input
                    System.out.println("Il movimento richiesto non è eseguibile");
                    printChoices();
                }
                break;
            case "B":   //devo creare messaggio per bomba
                if(manager.bombAvailable()){
                    //recupero la bomba
                    GridColor bomb = manager.useBomb();
                    //creo il messaggio
                    message = new Message(MessageType.BOMB_SPAWNED,gson.toJson(bomb));
                    //e lo salvo
                    manager.setMessageToSend(message);
                    break;
                }
                //non ho bombe. mi comport come in caso di default
            default:
                System.out.println("Input non valido\n");
                printChoices();
        }
    }
}

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class PeerRequestSender {

    //metodo per inviare una richiesta singola (ignoro risposta)
    public static Socket sendRequest(String message, Player destination){
        try{
            //creo socket e streamer
            Socket socket = new Socket(destination.getAddress(), destination.getPort());
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            //invio dati
            outToServer.writeBytes(message + "\n");
            outToServer.flush();
            //restituisco la socket
            return socket;
        }catch (Exception e){
            System.err.println("Errore nel tentativo di comunicare con un altro client");
            System.err.println("------------------------------------------------------");
            e.printStackTrace();
            return null;
        }
    }

    //invia piu' messaggi in parallelo (ignoro risposta)
    public static void sendRequestToAll(ArrayList<Player> players, Player me, String message) {
        ArrayList<Sender> threads = new ArrayList<>();
        //invio il messaggio a tutti
        for (Player destination : players) {
            //tranne che a me stesso
            if(!destination.equals(me)) {
                //avvio thread che mandera' messaggio
                Sender send = new Sender(destination,message);
                threads.add(send);
                send.start();
            }
        }
        //attendo che ogni thread abbia finito
        for(Sender send: threads){
            try{
                send.join();
            }catch (Exception e){
                System.err.println("Errore nell'attesa che i thread inviassero un messaggio a più destinatari");
                System.err.println("-------------------------------------------------------------------------");
                e.printStackTrace();
            }
        }
    }

    //invia piu' messaggi in parallelo (attende e restituisce risposta)
    public static ArrayList<Socket> sendRequestToAllWaiting(ArrayList<Player> players, Player me, String message){
        ArrayList<Thread> threads = new ArrayList<>();
        ArrayList<Socket> sockets = new ArrayList<>();
        //avvio thread
        int i = 0;
        for(Player p: players){
            if(!p.equals(me)){
                SenderWithSocket thread = new SenderWithSocket(p,message,sockets,i);
                threads.add(thread);
                thread.start();
                i++;
            }
        }
        //attendo che i thread abbiano finito
        for(Thread t : threads){
            try{
                t.join();
            }catch (Exception e){
                System.err.println("Errore nell'attesa che un thread finisse di inviare dati");
                System.err.println("--------------------------------------------------------");
                e.printStackTrace();
            }
        }
        return sockets;
    }

    //metodo che esegue chiamate a tutti i peer per poter correttamente entrare in partita
    public static PlayerCoordinate spawnPlayer(ArrayList<Player> players, Player me, int dimension){
        boolean spawned = true;
        Gson gson = new Gson();
        PlayerCoordinate coordinate;
        ArrayList<PlayerCoordinate> oldAttempts = new ArrayList<>();
        //finche' non riesco a spawnare
        do{
            //creo coordinate e le metto in un messaggio
            coordinate = spawnCoordinate(dimension, oldAttempts);
            oldAttempts.add(coordinate);
            Message toSend = new Message(MessageType.CHECK_SPAWN,gson.toJson(coordinate));
            for (Player destination : players) {
                if (!destination.equals(me)) {
                    try {
                        Socket socket = PeerRequestSender.sendRequest(gson.toJson(toSend),destination);
                        //attendo risposta
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        //valuto risposta
                        String answer = reader.readLine();
                        spawned = gson.fromJson(answer, boolean.class);
                        //chiudo socket
                        socket.close();
                    } catch (Exception e) {
                        System.err.println("Errore nel tentativo di comunicare coi peer per spawnare");
                        System.err.println("--------------------------------------------------------");
                        e.printStackTrace();
                    }
                }
            }
        }while(!spawned);
        return coordinate;
    }

    //crea coordinate random per lo spawn
    private static PlayerCoordinate spawnCoordinate(int dimension, ArrayList<PlayerCoordinate> old){
        Random generator = new Random();
        PlayerCoordinate actual = new PlayerCoordinate(generator.nextInt(dimension),generator.nextInt(dimension));
        while(old.contains(actual)) actual = new PlayerCoordinate(generator.nextInt(dimension),generator.nextInt(dimension));
        return actual;
    }

    //metodo per inviare un movimento agli altri player. Restituisce true se la mossa uccide un player
    public static boolean sendMove(ArrayList<Player> players, Player me, String message){
        ArrayList<Socket> sockets = sendRequestToAllWaiting(players, me, message);
        //numero di uccisioni eseguite (al massimo diventera' 1)
        int killed = 0;
        //controllo le risposte
        for(int i = 0; i < sockets.size(); i++){
            Socket socket = sockets.get(i);
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //valuto risposta
                String answer = reader.readLine();
                Message m = new Gson().fromJson(answer,Message.class);
                if(m.getType() == MessageType.KILL_CONFIRMED){  //hai ucciso il player
                    GameplayManager.getIstance().addEvent("Hai mangiato " + players.get(i).getName() + "!");
                    killed++;
                    //non essendoci piu' persone sulla stessa coordinata, esco dal ciclo
                    break;
                }
                //chiudo la socket
                socket.close();
            }catch (Exception e){
                System.err.println("Errore nella lettura della risposta di un thread dell'invio di un movimento");
                System.err.println("---------------------------------------------------------------------------");
                e.printStackTrace();
            }
        }
        return killed == 1;
    }

    //metodo che risponde ad una socket gia' aperta da altri client
    public static void answer(Socket socket, String message) {
        try{
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            //invio dati
            outToServer.writeBytes(message + "\n");
            outToServer.flush();
        }catch (Exception e){
            System.err.println("Errore nel tentativo di rispondere ad un messaggio");
            System.err.println("--------------------------------------------------");
            e.printStackTrace();
        }
    }
}

class Sender extends Thread{
    protected Player destination;
    protected String message;

    public Sender(Player destination, String message) {
        this.destination = destination;
        this.message = message;
    }

    @Override
    public void run() {
        Socket sock = PeerRequestSender.sendRequest(message,destination);
        try{
            sock.close();
            System.out.println("Ho chiuso una socket, spero di non rompere nulla");
        }catch (Exception e){
        System.err.println("Errore nella chiusura della socket nel thread Sender");
        System.err.println("----------------------------------------------------");
        e.printStackTrace();
        }
        }
}

class SenderWithSocket extends Sender{
    private ArrayList<Socket> sockets;
    private int position;

    public SenderWithSocket(Player destination, String message, ArrayList<Socket> sockets, int position) {
        super(destination, message);
        this.sockets = sockets;
        this.position = position;
    }

    @Override
    public void run() {
        sockets.add(position, PeerRequestSender.sendRequest(message,destination));
    }
}

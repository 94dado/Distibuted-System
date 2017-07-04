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
    private static void sendRequestToAll(ArrayList<Player> players, Player me, String message) {
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
    private static ArrayList<Socket> sendRequestToAllWaiting(ArrayList<Player> players, Player me, String message){
        ArrayList<Thread> threads = new ArrayList<>();
        ArrayList<Socket> sockets = new ArrayList<>();
        //avvio thread
        int i = 0;
        for(Player p: players){
            if(!p.equals(me)){
                SenderWithSocket thread = new SenderWithSocket(p,message,sockets);
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

    public static void sendToken(Player next, String message){
        PeerRequestSender.sendRequest(message,next);
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
                        //se fin'ora ho ottenuto ok, continuo ad aggiornare il booleano
                        if(spawned) spawned = gson.fromJson(answer, boolean.class);
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
    public static int sendMove(ArrayList<Player> players, Player me, String message){
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
                    GameplayManager.getIstance().addEvent("Hai mangiato " + new Gson().fromJson(m.getJsonMessage(),Player.class).getName() + "!");
                    killed++;
                }
                //chiudo la socket
                socket.close();
            }catch (Exception e){
                System.err.println("Errore nella lettura della risposta di un thread dell'invio di un movimento");
                System.err.println("---------------------------------------------------------------------------");
                e.printStackTrace();
            }
        }
        return killed;
    }

    public static int sendBomb(ArrayList<Player> players, String message){
        int killed = 0;
        final int maxKill = 3;
        Player me = GameplayManager.getIstance().getMe();
        ArrayList<Socket> sockets = sendRequestToAllWaiting(players,null,message);
        for(Socket s: sockets){
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                Message m = new Gson().fromJson(reader.readLine(),Message.class);
                if(m.getType() == MessageType.KILL_CONFIRMED){
                    if(killed < maxKill) killed++;
                    Player other = new Gson().fromJson(m.getJsonMessage(),Player.class);
                    if(other.equals(me))
                        GameplayManager.getIstance().addEvent("Ti sei suicidato");
                    else
                        GameplayManager.getIstance().addEvent("La tua bomba ha ucciso " + other.getName());
                }
            }catch (Exception e){
                System.err.println("Errore lettura risposta all'esplosione");
                System.err.println("--------------------------------------");
                e.printStackTrace();
            }
        }
        return  killed;
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

    public static void addNewPlayer(ArrayList<Player> players, Player me, String message) {
        sendRequestToAll(players,me,message);
    }

    public static void suicide(String message, Player me) {
        sendRequest(message,me);
    }

    public static void sendRemovePlayer(ArrayList<Player> players, Player pl, String message) {
        sendRequestToAll(players, pl, message);
    }

    public static ArrayList<Socket> spawnBomb(ArrayList<Player> playersList, String s) {
        return sendRequestToAllWaiting(playersList, null, s);
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
        }catch (Exception e){
        System.err.println("Errore nella chiusura della socket nel thread Sender");
        System.err.println("----------------------------------------------------");
        e.printStackTrace();
        }
    }
}

class SenderWithSocket extends Sender{
    private ArrayList<Socket> sockets;

    public SenderWithSocket(Player destination, String message, ArrayList<Socket> sockets) {
        super(destination, message);
        this.sockets = sockets;
    }

    @Override
    public void run() {
        sockets.add(PeerRequestSender.sendRequest(message,destination));
    }
}

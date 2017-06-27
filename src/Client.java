import com.google.gson.Gson;

import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Scanner;


public class Client {
    static private Scanner s;
    static private Player me;
    private static String url = "http://localhost:8080/gameServer/";
    private static Gson gson = new Gson();
    private static ServerSocket serverSocket;

    public static void main(String[] args){
        s = new Scanner(System.in);
        String ip;
        int port;
        System.out.println("Progetto Sistemi Distribuiti 2016/2017 - Davide Quadrelli\n");
        //todo decommentare a progetto finito le prossime righe
//        System.out.println("Inserisci indirizzo del server:");
//        String serverAddress = s.nextLine();
//        port = getInt("Inserisci porta del server:");
//        url = "http://"+serverAddress+":"+port+"/gameServer/";
        System.out.println("Inserisci nome utente:");
        String name = s.nextLine();
        //avvio server per le comunicazioni fra client
        try{
            ip = getIpAddress();
            serverSocket = new ServerSocket(0);
            port = serverSocket.getLocalPort();
            me = new Player(name,ip,port);
            startApplication();
        }catch (Exception e){
            System.err.println("Errore nell'apertura della socket di ascolto del client!");
            System.err.println("--------------------------------------------------------");
            e.printStackTrace();
        }
    }

    private static void startApplication(){
        boolean again = true;
        while(again){
            printMenu();
            String input = s.nextLine();
            again = doChoice(input);
        }
    }

    private static void printMenu(){
        System.out.println("Inserisci operazione da voler eseguire:");
        System.out.println("(L)ista match aperti");
        System.out.println("(N)uovo match");
        System.out.println("(C)ollegati ad un match esistente");
        System.out.println("(E)sci");
    }

    private static int getInt(String string){
        int n;
        do{
            System.out.println(string);
            try{
                n = Integer.parseInt(s.nextLine());
            }catch (Exception e){
                n = 0;
            }
        }while(n <= 0);
        return n;
    }

    private static boolean doChoice(String input){
        String matchName;
        Match match;
        switch (input.toUpperCase()){
            case "L":
                //stampa lista match
                try {
                    HTTPRequestCreator req = new HTTPRequestCreator("GET", MediaType.APPLICATION_FORM_URLENCODED,url + "getMatches");
                    String answer = req.getAnswer();

                    String[] listOfMatch = gson.fromJson(answer,String[].class);
                    if(listOfMatch.length == 0) System.out.println("Nessun match disponibile, è possibile crearne uno nuovo.\n");
                    for(int i = 0; i < listOfMatch.length; i++){
                        System.out.println(i+ ": "+listOfMatch[i]);
                    }
                }catch (Exception e){
                    System.err.println("Errore di connesione. Chiudo ...");
                    System.err.println("--------------------------------");
                    e.printStackTrace();
                    return false;
                }
                break;
            case "N":
                //recupero nome match
                System.out.println("Inserire nome match da creare:");
                matchName = s.nextLine();
                //recupero dimensione griglia
                int dim;
                while ((dim = getInt("Inserire dimensione griglia: (numero pari)")) %2 != 0);
                //recupero punteggio per vittoria
                int point = getInt("Inserire punteggio per la vittoria:");
                match = new Match(matchName,dim,point,me);
                //invio al server il nuovo match
                try{
                    HTTPRequestCreator req = new HTTPRequestCreator("POST", MediaType.APPLICATION_FORM_URLENCODED,url + "addMatch");
                    String json = gson.toJson(match);
                    HashMap<String,String> params = new HashMap<>();
                    params.put("match",json);
                    req.putParams(params);

                    String answer = req.getAnswer();
                    boolean response = gson.fromJson(answer,boolean.class);
                    if(response){
                        //match creato
                        System.out.println("Partita creata con successo");
                        //avvio match
                        GameplayManager manager = new GameplayManager(serverSocket, match, me);
                        manager.startTheMatch();
                        return false;
                    }else{
                        //match non creato
                        System.out.println("Errore nella creazione della partita. Cambiare nome e riprovare");
                        System.err.println("---------------------------------------------------------------");
                    }
                }catch (Exception e){
                    System.err.println("Errore di connessione.");
                    e.printStackTrace();
                }
                break;
            case "C":
                System.out.println("Inserire nome del match in cui si vuole entrare:");
                matchName = s.nextLine();
                //provo a connettermi alla partita
                try{
                    HTTPRequestCreator req = new HTTPRequestCreator("GET", MediaType.APPLICATION_FORM_URLENCODED,url + "getMatchDetail?name="+matchName);
                    String answer = req.getAnswer();
                    match = gson.fromJson(answer,Match.class);
                    System.out.println(match.toReadableString());
                    System.out.println("Sicuro di voler entrare? (y/n)");
                    answer = s.nextLine().toLowerCase();
                    if(answer.equals("y")){
                        req = new HTTPRequestCreator("PUT", MediaType.APPLICATION_FORM_URLENCODED,url + "addPlayerToMatch");
                        String json = gson.toJson(me);
                        HashMap<String,String> params = new HashMap<>();
                        params.put("player",json);
                        params.put("match",matchName);
                        req.putParams(params);

                        answer = req.getAnswer();
                        Match response = gson.fromJson(answer,Match.class);
                        //avvio match
                        GameplayManager manager = new GameplayManager(serverSocket, response, me);
                        manager.startTheMatch();
                        return false;
                    }else{
                        break;
                    }
                }catch (Exception e) {
                    System.err.println("Match non trovato");
                    System.err.println("-----------------");
                    e.printStackTrace();
                    break;
                }
            case "E":
                System.out.println("Chiusura in corso ...");
                return false;
            default:
                System.out.println("Inserisci qualcosa di sensato");
        }
        return true;
    }

    private static String getIpAddress(){
        String ip;
        try{
            ip = InetAddress.getLocalHost().getHostAddress();
        }catch (Exception e){
            ip = "";
        }
        return ip;
    }
}

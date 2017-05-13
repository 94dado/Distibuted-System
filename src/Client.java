import com.google.gson.Gson;

import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.util.Scanner;


public class Client {
    static private Scanner s;
    static private Player me;
    private static final String url = "http://localhost:8080/gameServer/";
    private static Gson gson = new Gson();

    public static void main(String[] args){
        s = new Scanner(System.in);
        System.out.println("Progetto Sistemi Distribuiti 2016/2017 - Davide Quadrelli\n");
        System.out.println("Inserisci nome utente:");
        String name = s.nextLine();
        String ip = getIpAddress();
        String port = /*args[0];*/ "9999";
        me = new Player(name,ip,port);
        startApplication();
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
                    for(int i = 0; i < listOfMatch.length; i++){
                        System.out.println(i+ ": "+listOfMatch[i]);
                    }
                }catch (Exception e){
                    System.err.println("Errore di connesione. Chiudo ...");
                    e.printStackTrace();
                    return false;
                }
                break;
            case "N":
                //recupero nome match
                System.out.println("Inserire nome match da creare:");
                matchName = s.nextLine();
                //recupero dimensione griglia
                int dim = getInt("Inserire dimensione griglia:");
                //recupero punteggio per vittoria
                int point = getInt("Inserire punteggio per la vittoria:");
                match = new Match(matchName,dim,point,me);
                //TODO send match to server
                break;
            case "C":
                System.out.println("Inserire nome del match in cui si vuole entrare:");
                matchName = s.nextLine();
                try{
                    HTTPRequestCreator req = new HTTPRequestCreator("GET", MediaType.APPLICATION_FORM_URLENCODED,url + "getMatchDetail?name="+matchName);
                    String answer = req.getAnswer();
                    match = gson.fromJson(answer,Match.class);
                    System.out.println(match.toReadableString());
                    System.out.println("Sicuro di voler entrare? (y/n)");
                    answer = s.nextLine().toLowerCase();
                    if(answer.equals("y")){
                        //TODO connect to the match
                    }else{
                        break;
                    }
                }catch (Exception e){
                    System.err.println("Match non trovato");
//                    e.printStackTrace();
                    break;
                }
                break;
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

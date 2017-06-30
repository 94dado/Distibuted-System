import java.net.ServerSocket;
import java.net.Socket;

public class ClientListenerThread extends Thread{

    @Override
    public void run() {
        ServerSocket serverSocket = GameplayManager.getIstance().getServerSocket();
        try{
            //finchè la partita è in corso
            while(!GameplayManager.getIstance().isMatchFinished()){
                //attendo messaggio
                Socket sock = serverSocket.accept();
                //avvio thread che elaborera' il messaggio
                MessageHandler handler = new MessageHandler(sock);
                handler.start();
            }
            //partita finita. chiudo la socket
            serverSocket.close();
        }catch (Exception e){
            System.err.println("Errore nel listener del client");
            System.err.println("------------------------------");
            e.printStackTrace();
        }
    }
}

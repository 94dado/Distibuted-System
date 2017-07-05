import java.net.ServerSocket;
import java.net.Socket;

public class ClientListenerThread extends Thread{
    private ServerSocket serverSocket;
    @Override
    public void run() {
        serverSocket = GameplayManager.getIstance().getServerSocket();
        try{
            //finchè la partita è in corso
            while(!GameplayManager.getIstance().isMatchFinished()){
                //attendo messaggio
                Socket sock = serverSocket.accept();
                //avvio thread che elaborera' il messaggio
                MessageHandler handler = new MessageHandler(sock);
                handler.start();
            }
        }catch (Exception e){
            //succede col suicidio della bomba. ripristino il thread. altri eventuali casi saranno da valutare
            run();
        }
    }

    public void stopListener(){
        try{
            if(!serverSocket.isClosed())serverSocket.close();
        }catch (Exception e){
            System.err.println("Errore nella chiusura della socket server");
            //se sono qui, sto comunque gia' morendo. proseguo e basta
        }
        this.stop();
    }
}

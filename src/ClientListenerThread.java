import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientListenerThread extends Thread{
    private GameplayManager manager;

    public ClientListenerThread (GameplayManager manager){
        this.manager = manager;
    }

    @Override
    public void run() {
        try{
            //avviso thread principale che sono pronto
            synchronized (manager.startLock){
                manager.startLock.notify();
            }
            while(!manager.isMatchFinished()){
                //attendo messaggio
                System.out.println("Sto in attesa di un messaggio");
                Socket sock = manager.getServerSocket().accept();
                //elaboro messaggio
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String message = inFromClient.readLine();
                String[] infos = message.split(" ");
                switch (infos[0]){
                    case GameplayManager.tokenMessage:   //ho ricevuto il token
                        synchronized (manager.tokenLock){
                            TokenRingThread thread = new TokenRingThread(manager);
                            thread.start();
                        }
                        break;
                    case GameplayManager.newPlayerMessage:   //nuovo player in partita
                        ClientNewPlayerThread addPlayerThread = new ClientNewPlayerThread(manager,infos[1]);
                        addPlayerThread.start();
                        break;
                    //todo da finire
                }
            }
        }catch (Exception e){
            System.err.println("Errore nel listener del client");
            System.err.println("------------------------------");
            e.printStackTrace();
        }
    }
}

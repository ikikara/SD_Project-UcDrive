
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Admin extends Remote{
    public String register(String data) throws RemoteException;
    public String list(String data) throws RemoteException;
    public String failover(String data) throws RemoteException;
    public String size(String data) throws RemoteException;
    public String duplicate() throws RemoteException;
}

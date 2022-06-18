
import java.net.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;
import java.util.NoSuchElementException;

public class adm {
    private static Admin h;
    private static String info = "";
    private static String data;
    private static Scanner sc;
    public static void main(String args[]) {
        sc = new Scanner(System.in);
        
        try {
            h = (Admin) LocateRegistry.getRegistry(7001).lookup("admin");
            System.out.print("Admin:\\> type 'help' for a list of possible commands!\n");
            while(info.compareTo("exit") != 0){
                System.out.print("Admin:\\> ");             
                info = sc.nextLine();

                switch(info){
                    case("register"):
                        register();
                        break;
                    case("list"):
                        list();
                        break;
                    case("failover"):
                        failover();
                        break;
                    case("size"):
                        size();
                        break;
                    case("copy"):
                        copy();
                        break;
                    case("help"):
                        help();
                        break;
                    case("exit"):
                        break;
                    default:
                        System.out.println("'" + info + "' is not recognized as an internal or external command\n");
                        break;
                }
            }
        } 
        catch (NullPointerException e){
            System.out.print("\n\nApplication closed abruptaly\n\n");
            System.exit(1);
        }
        catch (NoSuchElementException e){
            System.out.print("\n\nApplication closed abruptaly\n\n");
            System.exit(1);
        }
        catch (java.rmi.ConnectException e) {
            System.out.print("\nThe Server is currently down\n");
        }
        catch (RemoteException e){
            System.out.print("\nSomething went wrong when you tried to use this method\n");
        }
        catch (NotBoundException e){
            System.out.print("\nYou tried to lookup or unbind in the registry a name that has no associated binding");
        }
    }

    public static void register() throws RemoteException, NullPointerException{
        System.out.print("Please introduce the information relative to the new user:\n");
        data = new String(System.console().readLine("Username: ")) + "#";
        data += new String(System.console().readLine("Password: ")) + "#";
        data += new String(System.console().readLine("Department: ")) + "#";
        data += new String(System.console().readLine("Address: ")) + "#";
        data += new String(System.console().readLine("Phone number: ")) + "#";
        data += new String(System.console().readLine("Id: ")) + "#";
        System.out.print("ExpirationId date:\n");
        data += new String(System.console().readLine(" -> Day: ")) + "/";
        data += new String(System.console().readLine(" -> Month: ")) + "/";
        data += new String(System.console().readLine(" -> Year: "));
        data = h.register(data);
        System.out.print(data);
    }

    public static void list() throws RemoteException, NullPointerException{
        data = new String(System.console().readLine("Please introduce the name of the user: "));
        data = h.list(data);
        System.out.print(data);
    }

    public static void failover() throws RemoteException, NullPointerException{
        System.out.println("Please introduce the specifications of the heartbeat mecanism:");
        data = new String(System.console().readLine("Number of lost pings until server is down: ")) + "#";
        data += new String(System.console().readLine("Time between each ping (miliseconds): ")) + "#";
        data = h.failover(data);
        System.out.print(data);
    }

    public static void size() throws RemoteException, NullPointerException{
        System.out.println("Would you like to check the size of all files (type 'all') or of one user (type user name):");
        data = sc.nextLine();
        data = h.size(data);
        if(data.compareTo("ERROR") == 0){
            System.out.print("\nThe specified user or folder Data doesn't exist\n\n");
        }
        else{
            System.out.print("Size: " + data + " bytes\n\n");
        }
    }

    public static void copy() throws RemoteException, NullPointerException{
        System.out.println("Files present in primary server: ");
        data = h.list("all");
        System.out.print(data);
        System.out.println("Files present in secondary server: ");
        data = h.duplicate();
        System.out.print(data);
    }

    public static void help() throws RemoteException{
        System.out.println("\nHere's a list of available commands:");
        System.out.println("> register              Register a new user");
        System.out.println("> list                  List directories/file from an user");
        System.out.println("> failover              Config the failover mechanism");
        System.out.println("> size                  list the memory details");
        System.out.println("> copy                  see the replication data between two servers");
    }
}

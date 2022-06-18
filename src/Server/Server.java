
import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Server extends UnicastRemoteObject implements Admin{
    // 0 - port 1st Server | 1 - port 2nd Server | 2 - ip 1st Server | 3 - ip 2nd Server
    private static ArrayList<String> configs = load_configs();
    private static ArrayList<User> credentials = load_data();
    private static int maxfailed = Integer.parseInt(configs.get(4)), timeout = 500, bufsize = 4096, period = Integer.parseInt(configs.get(5));
    private static Registry r;
    private static Socket clientSocket;
    private static CheckUDP check;

    public Server() throws RemoteException {
        super();
    }
    public static void main(String args[]){
        hearbeats();

        check = new CheckUDP(configs.get(3), bufsize, Integer.parseInt(configs.get(1)));
        check.start();

        System.out.print("The server is now running as Primary!\n\n");

        try (ServerSocket listenSocket = new ServerSocket(Integer.parseInt(configs.get(0)), 0, InetAddress.getByName(configs.get(2)))) {
            //rmi
            try {
                r = LocateRegistry.createRegistry(7001);
                r.rebind("admin", new Server());
            } 
            catch (RemoteException re) {
                System.out.println("Exception in HelloImpl.main: " + re);
            }

            while(true) {
                clientSocket = listenSocket.accept();

                new Client_Connection(clientSocket, credentials);
            }
        }
        catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        }  
        catch(IOException e) {
            System.out.print("IP/Port already used, wait until they are free\n");
        }
    }

    // TCP FUNCTIONS
    public static void hearbeats(){
        int count = 0, failed = 0, n; 
        InetAddress ia;
        ByteArrayInputStream bais;
        ByteArrayOutputStream baos;
        DataInputStream dis;
        DataOutputStream dos;
        byte [] buf, rbuf = new byte[bufsize];
        DatagramPacket dr;
        DatagramSocket ds;

        // Heartbeat
        try{
            ia = InetAddress.getByName(configs.get(3));
            ds = new DatagramSocket();
            ds.setSoTimeout(timeout);
            //new BackUpReceiver(configs.get(3), 8000);

            while (failed < maxfailed) {
                try {
                    if(count == 10){
                        ReceiveUDP rudp = new ReceiveUDP("127.0.0.1", bufsize, 6500);
                        rudp.start();
                    }

                    baos = new ByteArrayOutputStream();
                    dos = new DataOutputStream(baos);
                    dos.writeInt(count++);
                    buf = baos.toByteArray();
                    ds.send(new DatagramPacket(buf, buf.length, ia, Integer.parseInt(configs.get(1))));
                    
                    dr = new DatagramPacket(rbuf, rbuf.length);
                    ds.receive(dr);

                    failed = 0;
                    bais = new ByteArrayInputStream(rbuf, 0, dr.getLength());
                    dis = new DataInputStream(bais);
                    n = dis.readInt();
                    System.out.println("Successful heartbeats: " + n + ". Primary Server is currently operational!");

                } 
                catch (SocketTimeoutException ste) {
                    failed++;
                    System.out.println("Failed heartbeats: " + failed + ". Preparing to assume Primary status!");
                }
                Thread.sleep(period);
            }
        } catch(Exception e){
            e.printStackTrace();
        } 
    }

    public static ArrayList<User> load_data(){
        File data_file = new File("Users.txt");
        ArrayList<User> data = new ArrayList<>();
        FileReader fr;
        BufferedReader br;
        String line;
        String [] user_info;
        User user;

        try{
            if(data_file.exists() && data_file.isFile()){
                fr = new FileReader(data_file);
                br = new BufferedReader(fr);
                while((line=br.readLine())!=null){
                    if(!line.isEmpty()){
                        user_info = line.split("#");
                        if(user_info.length == 8){
                            user = new User(user_info[0], user_info[1], user_info[2], user_info[3], user_info[4], user_info[5], user_info[6], user_info[7]);
                            data.add(user);
                        }
                    }
                }
            }
            else{
                System.out.print("Error: File not found\n");
                System.exit(1);
            }
        }
        catch (FileNotFoundException e){
            System.out.print("Error: File not found\n");
            System.exit(1);
        }
        catch (IOException e){
            System.out.print("Error: Input error\n");
        }
        /*for(User user2 : data){
            System.out.println(user2.getUsername());
        }*/

        return data;
    }

    public static ArrayList<String> load_configs(){
        File file = new File("Configs.txt");
        ArrayList<String> data = new ArrayList<>();
        FileReader fr;
        BufferedReader br;
        String line;
        
        try{
            if(file.exists() && file.isFile()){
                fr = new FileReader(file);
                br = new BufferedReader(fr);
                while((line=br.readLine())!=null){
                    if(!line.isEmpty()){
                        data.add(line);
                    }
                }
            }
        }
        catch (FileNotFoundException ex){
            System.out.print("ERROR: File not found\n");
        }
        catch (IOException e){
            System.out.print("ERROR: Input error\n");
        }

        return data;
    }

    // RMI FUNCTIONS
    synchronized public String register(String data) throws RemoteException{
        Writer output;
        String admin_path;
        File f;
        String[] keywords = data.split("#");
        UploadUDP uudp;

        if(keywords.length != 7){
            return "\nMissing information, some camps are blank\n\n";
        }

        if(check_existence(keywords[0])){
            return "\nUser already exists\n\n";
        }

        try {
            output = new BufferedWriter(new FileWriter("Users.txt", true));
            output.append("\n" + data + "#" + keywords[0] );
            output.close();
        }
        catch (IOException e){
            System.out.print("ERROR: Input error\n");
            return "\nIt wasn't possible to add new User\n\n";
        }

        admin_path = System.getProperty("user.dir") + "/Data/" + keywords[0];
        f = new File(admin_path);

        if(!f.mkdir()){
            return "\nIt wasn't possible to add new User\n\n";
        }

        uudp = new UploadUDP("127.0.0.1", 4096, 6500, "register#" + keywords[0], data);
        uudp.start();

        return "\nUser added sucessfully\n\n";
    }

    public String list(String data) throws RemoteException{
        String[] keywords = data.split("[*]");

        if(keywords[0].compareTo("secondary") == 0){
            try{
                File folder = new File(keywords[1]);
                
                return getFiles(folder, 0);
            }
            catch(IOException e){
                System.out.println(e);
            }
        }
        else if(data.equals("all")){
            try{
                File folder = new File(System.getProperty("user.dir") + "/Data");
                return getFiles(folder, 0);
            }
            catch(IOException e){
                System.out.println(e);
            }
        }
        else {
            try {
                if(!check_existence(data)){
                    return "The specified user doesn't exist\n\n";
                }

                File folder = new File(System.getProperty("user.dir") + "/Data/" + data);
                return getFiles(folder, 0);
            } 
            catch (IOException e) {
                System.out.println(e);
            }
        }

        return "";
    }

    public String failover(String data) throws RemoteException{
        String[] keywords = data.split("#");
        Path path;
        List<String> lines;

        if(keywords.length != 2){
            return "\nMissing information, some camps are blank\n\n";
        }

        try{
            path = Paths.get("Configs.txt");
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(4, keywords[0]);
            lines.set(5, keywords[1]);
            Files.write(path, lines, StandardCharsets.UTF_8);
        }
        catch (IOException e){
            System.out.print("Some error occurred while configuring heartbeat mecanism");
            return "\n\nError configuring heartbeat mecanism\n\n";
        }
        UploadUDP uudp = new UploadUDP("127.0.0.1", 4096, 6500, "config#" + data.split("#")[0], data);

        uudp.start();
        return "\nHeartbeat mecanism configured sucessfully\n\n";
    }

    public String size(String data) throws RemoteException{
        String client_path = System.getProperty("user.dir") + "/Data/";
        File file1;

        if(data.compareTo("all") != 0 && !check_existence(data)){
            return "ERROR";
        }

        if (data.compareTo("all") != 0){
            client_path += data;
        }

        file1 = new File(client_path);

        return Long.toString(getFolderSize(file1));
    }

    public String duplicate() throws RemoteException{
        try {
            UploadUDP uudp = new UploadUDP("127.0.0.1", 4096, 6500, "list#user", "");
            Thread thread = new Thread(uudp);
            thread.start();
            thread.join();
            String secondary_path = uudp.getSecondary_path();
            
            return list("secondary*" + secondary_path);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // verifications and auxiliar functions
    public static String getFiles(File folder, int ident) throws IOException{
        String file = "";
        File[] files = folder.listFiles();


        try{
            for (int i = 0; i < files.length; i++) {
                for(int j = 0; j<ident; j++){
                    
                    file += extendedAscii(186) + "  ";
                }

                if(i != files.length-1){
                    file += extendedAscii(204);
                }
                else{
                    file += extendedAscii(200);
                }

                file += extendedAscii(205) + files[i].getName() + "\n";
                

                if (files[i].isDirectory()) {
                    file += getFiles(files[i], ident + 1);
                }
            }
        }
        catch(Exception e){
            return "\nThe folder Data was deleted or doesn't exist\n\n";
        }
        
        return file;
    }

    public static long getFolderSize(File folder) {
        long length = 0;
        File[] files = folder.listFiles();
        int count = files.length;

        for (int i = 0; i < count; i++) {
            if (files[i].isFile()) {
                length += files[i].length();
            }
            else {
                length += getFolderSize(files[i]);
            }
        }
        return length;
    }

    public static boolean check_existence(String user){
        File file = new File(System.getProperty("user.dir") + "/Data/");

        for(File f : file.listFiles()){
            if(f.isDirectory() && user.compareTo(f.getName())==0){
                return true;
            }
        }

        return false;
    }
    
    public static char extendedAscii(int codePoint) throws UnsupportedEncodingException {
        return new String(new byte[] { (byte) codePoint }, "Cp437").charAt(0);
    }
}

class Client_Connection extends Thread {
    private ArrayList<User> credentials;
    private User me;
    private int line;
    private String server_path, domain_server_path, username = "[Unknown]";
    private DataInputStream in;
    private DataOutputStream out;
    private Socket clientSocket;

    public Client_Connection (Socket aClientSocket, ArrayList<User> credentials) {
        this.credentials = credentials;
        try{
            this.clientSocket = aClientSocket;
            this.in = new DataInputStream(clientSocket.getInputStream());
            this.out = new DataOutputStream(clientSocket.getOutputStream());
            this.start();
        } 
        catch(IOException e){
            System.out.println("Connection:" + e.getMessage());
        }
    }

    public void run(){
        String[] keywords = {""};
        String data = "";
        try {
            authentication();
            username = this.me.getUsername();

            while(keywords[0].compareTo("ARRIVEDERCI") != 0){
                data = this.in.readUTF();
                keywords = data.split(" ");

                switch(keywords[0]){
                    // to change password
                    case("CHANGEPASSWORD"):
                        changepassword(keywords);
                        break;
                    // to change IP/Port of Server
                    case("CONFIG"):
                        changeports(keywords);
                        break;
                    case("CREATEDIRECTORY"):
                        createdirectory(data.substring(keywords[0].length()+1, data.length()));
                        break;
                    // to list files on server
                    case("LISTFILES"):
                        listfiles();
                        break;
                    // to change server's directory
                    case("CHANGEDIRECTORY"):
                        changedirectory(data.substring(keywords[0].length()+1, data.length()));
                        break;
                    // to download a file
                    case("DOWNLOAD"):
                        downloadfiles(keywords[1]);
                        break;
                    // to upload a file
                    case("UPLOAD"):
                        uploadfiles(keywords[0]);
                        break;
                    // to loggout
                    case("ARRIVEDERCI"):
                        System.out.print("> " + this.me.getUsername() + ": " + "logout\n");
                        break;
                    default:                    
                        if(keywords[0].compareTo("ERROR") == 0){
                            data = this.in.readUTF();
                        }

                        System.out.print("> " + this.me.getUsername() + ": " + "introduce the invalid command '" + data + "'\n");
                        this.out.writeUTF("'" + data + "' is not recognized as an internal or external command\n\n");
                        break;
                }
            }

            this.clientSocket.close();
        } 
        catch(SocketException e){
            System.out.print("> " + username + ": lost his connection to the server\n");
        }
        catch(IOException e) {
            System.out.println("IO:" + e);
        }
    }

    //COMMANDS FUNCTIONS
    private void authentication() throws IOException{
        String receive, tosend = "REJECTED";
        String[] keywords;

        do {
            receive = this.in.readUTF();
            keywords = receive.split(" ");
            if(keywords.length > 1 &&  searching_user(keywords[0], keywords[1])){                
                tosend = "ACCEPTED " + this.me.getDirectory();
            }

            out.writeUTF(tosend);
        } while(tosend.substring(0,8).compareTo("ACCEPTED") != 0);
        
        System.out.print("> " + this.me.getUsername() + ": login\n");
        this.domain_server_path = this.me.getDirectory();
        this.server_path = System.getProperty("user.dir") + "/Data/" + this.domain_server_path;
    }

    private void changepassword(String[] keywords) throws IOException{
        if(keywords.length != 4){
            System.out.print("> " + this.me.getUsername() + ": " + "introduced invalid information while trying to change password\n");
            out.writeUTF("\nMissing information, some camps are blank\n\n");

            return;
        }

        if(keywords[1].compareTo(this.me.getPassword()) == 0){
            if(keywords[2].compareTo(keywords[3]) == 0){
                change_info(keywords[2], 1, 0);
                System.out.print("> " + this.me.getUsername() + ": " + "changed his password\n");
                out.writeUTF("\nPassword sucessfully changed!\n\n");
                UploadUDP uudp = new UploadUDP("127.0.0.1", 4096, 6500, "changepass#", this.me.toString() + "*" + this.line);

                uudp.start();

                return;
            }
            else{
                System.out.print("> " + this.me.getUsername() + ": " + "failed to change his password\n");
                out.writeUTF("\nThe passwords introduced are not equal, please try again.\n\n");

                return;
            }
        }
        else{
            System.out.print("> " + this.me.getUsername() + ": " + "failed to change his password\n");       
            out.writeUTF("\nWrong password, please try again.\n\n");
            
            return;
        }
    }

    private void changeports(String[] keywords) throws IOException{
        int possible_error = parseIntError(keywords[1]);

        try{
            if(possible_error < 5 && possible_error > 0){
                if(validIpOrPort(possible_error, keywords[2])){
                    change_info(keywords[2], 2, Integer.parseInt(keywords[1])-1);

                    System.out.println("> " + this.me.getUsername() + ": " + "changed the ports/ip");
                    out.writeUTF("\nPort/IP sucessfully changed\n\n");
                }
                else{
                    System.out.println("> " + this.me.getUsername() + ": " + "introduced an invalid option port/ip while changing the ports/ip");
                    out.writeUTF("\nPort/IP invalid\n\n");
                }
            }
            else{
                System.out.println("> " + this.me.getUsername() + ": " + "introduced an invalid option while changing the ports/ip");
                out.writeUTF("\nOption introduced invalid\n\n");
            }
        }
        catch (IOException e){
            System.out.print("ERROR: Input error\n");
        }
    }

    private void createdirectory(String directory) throws IOException{
        directory = System.getProperty("user.dir") + "/Data/" + this.domain_server_path + "/" + directory;
        File file = new File(directory);

        if(file.mkdir()){
            UploadUDP uudp = new UploadUDP("127.0.0.1", 4096, 6500, "mkdir#", directory);

            uudp.start();

            out.writeUTF("SUCCESS");
        }
        else{
            out.writeUTF("FAILED");
        }
    }

    private void listfiles() throws IOException{
        File folder = new File(this.server_path);
        File[] files = folder.listFiles();
        String tosend = "";

        if (files == null || files.length == 0){
            out.writeUTF("ERROR");
            System.out.print("> " + this.me.getUsername() + ": " + "tried to list server files but none were found\n");
        } 
        else {
            for (int i = 0; i < files.length; i++){
                if (files[i].isDirectory()){
                    tosend += files[i].getName() + "D ";
                }
                else{
                    tosend += files[i].getName() + "F ";
                }
            }
            
            System.out.print("> " + this.me.getUsername() + ": " + "listed the files present in the current directory\n");
            out.writeUTF("SUCCESS " + tosend);  
        }
    }

    private void changedirectory(String directory) throws IOException{
        String[] path = directory.split("/");
        String original_path = this.domain_server_path;
        int last_folder;
        
        if(directory.charAt(0) != '/'){
            for(int i=0; i<path.length; i++){    
                if(path[i].compareTo("..") == 0){
                    last_folder = original_path.lastIndexOf("\\");
                    if(last_folder == -1){
                        System.out.print("> " + this.me.getUsername() + ": " + "try to change server directory out of his domain\n");
                        out.writeUTF("You don't have permission to navigate to here\n\n");

                        return;
                    }
                    else{
                        original_path = original_path.substring(0, last_folder);
                    }
                }   
                else{
                    if(!path[i].matches("[.]+")){
                        if(!directoryexist(original_path, path[i])){
                            System.out.print("> " + this.me.getUsername() + ": " + "try to change server directory but the folder doesn't exist\n");
                            out.writeUTF("The system cannot find the path specified\n\n");  

                            return;
                        }               
                    
                        original_path += "\\" + path[i];
                    }    
                }
            }

            this.domain_server_path = original_path;
            this.server_path = System.getProperty("user.dir") + "/Data/" + this.domain_server_path;
            change_info(this.domain_server_path, 0, 0);

            System.out.print("> " + this.me.getUsername() + ": " + "changed his server directory to " + this.domain_server_path + "\n");
            out.writeUTF("SUCCESS " + this.domain_server_path);

            return;
        }

        System.out.print("> Try to change server directory but the folder doesn't exist\n");
        out.writeUTF("The system cannot find the path specified\n\n");  
    } 

    private void downloadfiles(String name_file) throws IOException{
        File file = new File(server_path + "/" + name_file);

        if (file.exists()){
            out.writeUTF("AVAILABLE");
            new Download(this.me.getUsername(), name_file, server_path);
        } 
        else{
            System.out.print("> " + this.me.getUsername() + ": " + "tried to download a file but it wasn't found\n");
            out.writeUTF("NOTAVAILABLE");
        }

    }

    private void uploadfiles(String name_file) throws IOException{
        name_file = in.readUTF(); 
        new Upload(this.me.getUsername(), name_file, server_path);

    }

    // AUXILIAR FUNCTIONS 
    private boolean searching_user(String username, String password){
        int i=0;
        for(User user : this.credentials){
            if(user.getUsername().compareTo(username) == 0){
                if(user.getPassword().compareTo(password) == 0){
                    this.me = user;
                    this.line = i;
                    return true;
                }
            }            
            i++;
        }
        return false;
    }    

    private boolean directoryexist(String actualdirectory, String directory){
        File folder = new File(System.getProperty("user.dir") + "/Data/" + actualdirectory);
        File[] files = folder.listFiles();

        if(files != null){
            for(File file: files){
                if(file.isDirectory() && file.getName().compareTo(directory) == 0){
                    return true;
                }
            }
        }

        return false;
    }

    private boolean validIpOrPort(int option, String iporport){
        int possible_error;
        int[] ipdivided;

        if(option == 1 || option == 2){
            possible_error = parseIntError(iporport);

            return possible_error > 1023 && possible_error < 49152;
        }
        else{
            if(iporport.matches("([0-9]{1,3}.){3}[0-9]{1,3}")){
                ipdivided = Arrays.asList(iporport.split("[.]")).stream().mapToInt(Integer::parseInt).toArray();
                
                return (ipdivided[0] > -1 && ipdivided[0] < 248) &&
                       (ipdivided[1] > -1 && ipdivided[1] < 256) &&
                       (ipdivided[2] > -1 && ipdivided[2] < 256) &&
                       (ipdivided[3] > 0 && ipdivided[3] < 255);
            }
            else{
                return false;
            }
        }
    }

    synchronized private void change_info(String info, int flag, int file_line){
        try{
            Path path;

            if(flag == 2){
                path = Paths.get("Configs.txt");
            }
            else{
                path = Paths.get("Users.txt");
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if(flag == 2){
                lines.set(file_line, info);
            }
            else{
                if(flag == 1){
                    this.me.setPassword(info);
                }
                else{
                    this.me.setDirectory(info);
                }
                lines.set(this.line, this.me.toString());
            }
            
            Files.write(path, lines, StandardCharsets.UTF_8);
        }
        catch (IOException e){
            System.out.print("ERROR: Input error\n");
        }
    }
    
    private int parseIntError(String possible_num){
        try{
            return Integer.parseInt(possible_num);
        }
        catch (NumberFormatException e){
            return 0;
        }
    }

}

class Upload extends Thread{
    private ServerSocket sDU;
    private FileOutputStream fout; 
    private Socket clientSocket1;
    private String user;
    private String name_file;
    private String server_path;

    public Upload(String user, String name_file, String server_path){
        this.user = user;
        this.name_file = name_file;
        this.server_path = server_path;
        this.start();
    }

    public void run(){
        int bytes = 0;
        byte[] total_buffer = new byte[4*1024];
        DataInputStream din;
        long size;
        UploadUDP uudp;

        try{
            this.fout = new FileOutputStream(server_path + "/" + name_file);
            this.sDU = new ServerSocket(6001);
            this.clientSocket1 = this.sDU.accept();
            din = new DataInputStream(clientSocket1.getInputStream());
            size = din.readLong();
            
            while (size > 0 && (bytes = din.read(total_buffer, 0, (int)Math.min(size, total_buffer.length))) != -1) {
                fout.write(total_buffer,0,bytes);
                size = size - bytes;                           
            }
            
            this.fout.close();
            this.sDU.close();

            System.out.print("> " + user + ": " + "successfully uploaded a file!\n");

            uudp = new UploadUDP("127.0.0.1", 4096, 6500, server_path + "/" + name_file, "");
            uudp.start();
        }
        catch(IOException e){
            File file = new File(String.format(server_path + "/" + name_file));
            try{
                fout.close();
                try{
                    sDU.close();
                } catch (IOException f){
                    f.printStackTrace();
                }  
            } 
            catch (IOException f){
                f.printStackTrace();
            }  
                       
            if(file.delete()){
                System.out.print("> " + user + ": " + "failed while uploading a file! File successfully deleted!\n");
            }
            else{
                System.out.print("> " + user + ": " + "failed while uploading a file! File cannot be deleted!\n");
            }
        }
    }
}

class Download extends Thread{
    private ServerSocket sDU;
    private FileInputStream fin; 
    private Socket clientSocket1;
    private String user;
    private String name_file;
    private String server_path;

    public Download(String user, String name_file, String server_path){
        this.user = user;
        this.name_file = name_file;
        this.server_path = server_path;
        this.start();
    }

    public void run(){
        int bytes = 0;
        byte[] total_buffer = new byte[4*1024];
        DataOutputStream dout;
        File file = new File(server_path + "/" + name_file);

        try{
            this.fin = new FileInputStream(file);
            this.sDU = new ServerSocket(6001);
            this.clientSocket1 = this.sDU.accept();
            dout = new DataOutputStream(clientSocket1.getOutputStream());
            dout.writeLong(file.length());  
            
            while ((bytes=fin.read(total_buffer))!=-1){
                dout.write(total_buffer,0,bytes);
                dout.flush();
            }

            this.fin.close();
            this.sDU.close();

            System.out.print("> " + user + ": " + "successfully downloaded a file!\n");
        }
        catch(IOException e){
            System.out.print("> " + user + ": " + "failed while downloading a file! File might be corrupted!\n");
        }
    }
}

class CheckUDP extends Thread{
    private String ip;
    private int bufsize;
    private int port;
    private DatagramSocket ds;
    private DatagramPacket dp;
    private DatagramPacket dpresp;
    private ByteArrayInputStream bais;
    private ByteArrayOutputStream baos;
    private DataInputStream dis;
    private DataOutputStream dos;
    private byte buf[], resp[];

    public CheckUDP(String ip, int bufsize, int port){
        this.ip = ip;
        this.bufsize = bufsize;
        this.port = port;
    }

    public void run(){
        int count;

        try{
            this.ds = new DatagramSocket(port);

            while (true) {
                buf = new byte[bufsize];
                this.dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(this.ip), this.port);
                this.ds.receive(this.dp);
                this.bais = new ByteArrayInputStream(buf, 0, dp.getLength());
                this.dis = new DataInputStream(this.bais);
                count = this.dis.readInt();

                this.baos = new ByteArrayOutputStream();
                this.dos = new DataOutputStream(this.baos);
                this.dos.writeInt(count);
                resp = baos.toByteArray();
                this.dpresp = new DatagramPacket(resp, resp.length, this.dp.getAddress(), this.dp.getPort());
                this.ds.send(this.dpresp);
            }
        } 
        catch (IOException e){
            System.out.print("Can't initialize heartbeats because IP/Port are already in use\n");
        }
    }  

}

class UploadUDP extends Thread{
    private String ip;
    private String path;
    private String data;
    private int bufsize;
    private int port;
    private DatagramSocket ds, ds2;
    private volatile String secondary_path = System.getProperty("user.dir") + "/Data/";

    public UploadUDP(String ip, int bufsize, int port, String path, String data){
        this.ip = ip;
        this.bufsize = bufsize;
        this.port = port;
        this.path = path;
        this.data = data;
    }

    public String getSecondary_path() {
        return secondary_path;
    }

    public void run(){
        String[] keywords = path.split("#");

        if(keywords[0].equals("register")){
            register(path);
        }
        else if(keywords[0].equals("config")){
            config(path);
        }
        else if(keywords[0].equals("list")){
            list(path);
        }
        else if(keywords[0].equals("mkdir")){
            mkdir(data);
        }
        else if(keywords[0].equals("changepass")){
            changepass(data);
        }
        else {
            replicate(path);
        }
    }

    private void register(String path){
        byte[] packet;
        DatagramPacket send;

        try{
            this.ds = new DatagramSocket();
            packet = path.getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            this.ds.send(send);
            packet = this.data.getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            this.ds.send(send);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void list(String path){
        byte[] packet, buffer;
        DatagramPacket send2, receive;

        try{
            ds = new DatagramSocket();
            packet = path.getBytes();
            buffer = new byte[this.bufsize];
            send2 = new DatagramPacket(packet, packet.length, InetAddress.getByName(ip), port);
            ds.send(send2);
            ds2 = new DatagramSocket(6300);
            receive = new DatagramPacket(buffer,buffer.length, InetAddress.getByName(ip), 6300);
            ds2.receive(receive);
            secondary_path = new String(receive.getData(), 0, receive.getLength());
            ds2.close();
        }
        catch (Exception e){
            System.out.print("Port already in use");
        }
    }

    private void config(String path){
        byte[] packet;
        DatagramPacket send;

        try{
            this.ds = new DatagramSocket();
            packet = path.getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            this.ds.send(send);
            packet = this.data.getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            this.ds.send(send);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void mkdir(String path){
        String[] aux;
        int index;
        byte[] packet;
        DatagramPacket send;

        try{
            aux = path.split("/|\\\\");

            for(index=0; index < aux.length; index++){
                if(aux[index].compareTo("Data") == 0){
                    break;
                }
            }
            path = "";

            for(int j=index+1; j < aux.length; j++){
                path += aux[j] + "/";
            }

            this.ds = new DatagramSocket();
            packet = "mkdir#".getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            this.ds.send(send);

            packet = path.getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            ds.send(send);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void changepass(String pass){
        byte[] packet;
        DatagramPacket send;

        try{
            this.ds = new DatagramSocket();
            packet = "changepass#".getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            this.ds.send(send);

            packet = pass.getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            ds.send(send);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void replicate(String path){
        File file;
        byte[] packet;
        DatagramPacket send;
        String[] aux;
        int byyyyte, index;
        FileInputStream fin;

        try {
            ds = new DatagramSocket();
            file = new File(path);
            aux = path.split("/|\\\\");

            for(index=0; index < aux.length; index++){
                if(aux[index].compareTo("Data") == 0){
                    break;
                }
            }
            path = "";

            for(int j=index+1; j < aux.length; j++){
                path += aux[j] + "/";
            }
            
            packet = path.getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            ds.send(send);

            packet = ("" + file.length()).getBytes();
            send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), this.port);
            ds.send(send);

            packet = new byte[this.bufsize];
            fin = new FileInputStream(file);

            while ((byyyyte = fin.read(packet))!=-1) {
                send = new DatagramPacket(packet, byyyyte, InetAddress.getByName(this.ip), this.port);
                ds.send(send);
            }

            fin.close();

        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ReceiveUDP extends Thread{
    private String ip;
    private int bufsize;
    private int port;
    private DatagramSocket ds;

    public ReceiveUDP(String ip, int bufsize, int port){
        this.ip = ip;
        this.bufsize = bufsize;
        this.port = port;
        try{
            this.ds = new DatagramSocket(port);
        }
        catch(IOException e){
            System.out.print("Port already used\n");
        }
    }

    public void run(){
        byte[] buffer;
        String filename;
        DatagramPacket receive;
        String[] keywords;

        while(true){
            try{
                buffer = new byte[this.bufsize];
                receive = new DatagramPacket(buffer,buffer.length);
                this.ds.receive(receive);
                filename = new String(receive.getData(), 0, receive.getLength());
                keywords = filename.split("#");
                
                //for user folder
                if(keywords[0].equals("register")){
                    register(filename);
                }
                else if(keywords[0].equals("config")){
                    config(filename);
                }
                else if(keywords[0].equals("list")){
                    list();
                }
                else if(keywords[0].equals("mkdir")){
                    mkdir(filename);
                }
                else if(keywords[0].equals("changepass")){
                    changepass();
                }
                else {
                    replicate(filename);
                }
            }
            catch (IOException e){
                System.out.print("IP/Port already in use or not open, the mecanism of backup can't be initialize\n");
            }
        }
    }  

    private void register(String filename) throws IOException{
        String user_path;
        File f;
        Writer output;
        DatagramPacket receive;
        byte[] buffer = new byte[this.bufsize];

        user_path = System.getProperty("user.dir") + "/Data/" + filename.split("#")[1];
        f = new File(user_path);
        
        receive = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.ip), this.port);
        this.ds.receive(receive);
        filename = new String(receive.getData(), 0, receive.getLength());
        
        output = new BufferedWriter(new FileWriter("Users.txt", true));
        output.append("\n" + filename);
        output.close();

        if(!f.mkdir()){
            System.out.println("It wasn't possible to add new User");
        }
        System.out.println("User added sucessfully");
    }

    private void config(String filename) throws IOException{
        DatagramPacket receive;
        byte[] buffer = new byte[this.bufsize];
        String[] keywords;
        Path path;
        List<String> lines;
        
        receive = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.ip), this.port);
        this.ds.receive(receive);
        filename = new String(receive.getData(), 0, receive.getLength());
        keywords = filename.split("#");
        try{
            path = Paths.get("Configs.txt");
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(4, keywords[0]);
            lines.set(5, keywords[1]);
            Files.write(path, lines, StandardCharsets.UTF_8);
        }
        catch (IOException e){
            System.out.print("ERROR: Input error\n");
        }
        System.out.println("\nHeartbeat mecanism configured sucessfully\n\n");
    }

    private void list() throws IOException{
        String user_path;
        byte[] packet;
        DatagramSocket ds2;
        DatagramPacket send;

        user_path = System.getProperty("user.dir") + "/Data";
        packet = user_path.getBytes();

        ds2 = new DatagramSocket();
        send = new DatagramPacket(packet, packet.length, InetAddress.getByName(this.ip), 6300);
        ds2.send(send);
        ds2.close();
    }

    private void mkdir(String filename) throws IOException{
        DatagramPacket receive;
        byte[] buffer = new byte[this.bufsize];
        File file;

        receive = new DatagramPacket(buffer,buffer.length);
        this.ds.receive(receive);
        filename = new String(receive.getData(), 0, receive.getLength());

        file = new File(System.getProperty("user.dir") + "/Data/" + filename);

        if(!file.mkdir()){
            System.out.print("It wasn't possible to create the required directory");
        }
    }

    private void changepass() throws IOException{
        DatagramPacket receive;
        byte[] buffer = new byte[this.bufsize];
        String pass;
        Path path;
        List<String> lines;
        String[] keywords;

        receive = new DatagramPacket(buffer,buffer.length);
        this.ds.receive(receive);
        pass = new String(receive.getData(), 0, receive.getLength());
        keywords = pass.split("[*]");

        System.out.println(pass);

        try{
            path = Paths.get("Users.txt");
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(Integer.parseInt(keywords[1]), keywords[0]);
            Files.write(path, lines, StandardCharsets.UTF_8);
        }
        catch (IOException e){
            System.out.print("ERROR: Input error\n");
        }

    }

    private void replicate(String filename) throws IOException{
        DatagramPacket receive;
        byte[] buffer = new byte[this.bufsize];
        long size;
        FileOutputStream fout;

        receive = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), port);
        ds.receive(receive);
        size = Long.parseLong(new String(receive.getData(), 0, receive.getLength()));

        buffer = new byte[bufsize];
        fout = new FileOutputStream(System.getProperty("user.dir") + "/Data/" + filename);

        while (size > 0) {
            receive = new DatagramPacket(buffer, buffer.length);
            ds.receive(receive);
            fout.write(buffer, 0, receive.getLength());

            size -= buffer.length;
        }

        fout.close();
    }    
}   

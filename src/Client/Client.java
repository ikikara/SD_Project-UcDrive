
import java.net.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

import java.io.*;

public class Client {
    private static int port = 0, error = 0;
    private static String domain_server = "", domain_client = "Data", ip;
    private static String client_path = System.getProperty("user.dir") + '\\' + domain_client;
    private static DataInputStream in; 
    private static DataOutputStream out; 
    private static Socket s;
    private static String username, password;

    public static void main(String args[]) {
        String data = "", receive;
        String[] keywords; 

        while(true){
            introduceconfigs();

            try {
                s = new Socket("127.0.0.1", port);
                in = new DataInputStream(s.getInputStream());
                out = new DataOutputStream(s.getOutputStream());
                System.out.println();

                Scanner sc = new Scanner(System.in);
                    // authentication
                    authentication();
                    
                    //Menu
                    while (true){
                        System.out.print("Client:\\" + domain_client + "[Server:\\" + domain_server + "]>");
                        data = sc.nextLine();
                        keywords = data.split(" ");

                        try{
                            switch(keywords[0]){
                                // to change password
                                case("changepass"):
                                    changepassword(data);
                                    break;
                                // to change IP/Port of Server
                                case("config"):
                                    changeports(data);
                                    break;
                                case("mkdir"):
                                    createdirectory(data);
                                    break;
                                // to list files on server
                                case("lsS"):
                                    listserverfiles(data);  
                                    break;
                                // to change server's directory
                                case("cdS"):
                                    changeserverdirectory(data);
                                    break;
                                // to list files on client
                                case("ls"):
                                    listclientfiles();
                                    break;
                                // to change client's directory
                                case("cd"):
                                    changeclientdirectory(data);
                                    break;
                                // to download a file
                                case("downloadS"):
                                    downloadfiles(data, "Client:\\" + domain_client + "[Server:\\" + domain_server + "]>");
                                    break;
                                // to upload a file
                                case("uploadS"):
                                    uploadfiles(data, "Client:\\" + domain_client + "[Server:\\" + domain_server + "]>");
                                    break;
                                // to call an ambulance
                                case("help"):
                                    help();
                                    break;
                                // to loggout
                                case("exit"):
                                    sc.close();
                                    out.writeUTF("ARRIVEDERCI");
                                    System.exit(0);
                                    break;
                                default:
                                    out.writeUTF(data);
                                    receive = in.readUTF();
                                    System.out.print(receive);
                                    break;
                            }
                        }
                        catch (Exception e){
                            filetransfer_error();
                        }
                    }
            }     
            catch(NullPointerException e){
                System.out.print("\n\nApplication closed abruptaly\n");
                System.exit(1);
            }
            catch (NoSuchElementException e){
                System.out.print("\n\nApplication closed abruptaly\n\n");
                System.exit(1);
            }
            catch (UnknownHostException e) {
                System.out.println("Sock:" + e.getMessage());
            } 
            catch (EOFException e) {
                System.out.println("EOF:" + e.getMessage());
            } 
            catch (IOException e) {
                System.out.print("\nIP/Port don't correspond UCDrive IP/Port or the server is down\n\n");
            } 
        } 
    }

    // COMMANDS FUNCTIONS
    public static void introduceconfigs(){
        try{
            ip = new String(System.console().readLine("Choose the ip to connect to the server: "));
            port = parseIntError(new String(System.console().readLine("Choose the port to connect to the server: ")));
        }
        catch(NullPointerException e){
            System.out.print("\n\nApplication closed abruptaly\n");
            System.exit(1);
        }
    }

    public static void authentication() throws IOException, NullPointerException{
        String receive;
        String[] keywords;
        
        username = new String(System.console().readLine("Welcome to ucDrive!\nUsername: "));
        password = new String(System.console().readPassword("Password: "));
        out.writeUTF(username + " " + password);
        keywords = in.readUTF().split(" ");
        receive = keywords[0];

        if(keywords.length !=1){
            domain_server = keywords[1];
        }

        while (receive.compareTo("ACCEPTED") != 0) {
            username = new String(System.console().readLine("\nIncorrect login credentials, please enter them again!\n\nUsername: "));
            password = new String(System.console().readPassword("Password: "));        
            out.writeUTF(username + " " + password);
            keywords = in.readUTF().split(" ");
            receive = keywords[0];

            if(keywords.length !=1){
                domain_server = keywords[1];
            }
        }

        System.out.printf("\nWelcome back, %s!\n\n", username);
    }

    public static void changeports(String data) throws IOException, NullPointerException{
        String[] keywords = data.split(" ");
        
        if(keywords.length != 1){
            out.writeUTF("ERROR");
            out.writeUTF(data);
            System.out.print(in.readUTF());
        }
        else{
            data = "CONFIG";
            System.out.println("What do you want to change:");
            System.out.println("[1] Primary Server Port");
            System.out.println("[2] Secondary Server Port");
            System.out.println("[3] Primary Server IP");
            System.out.println("[4] Secondary Server IP\n");
            data += " " + new String(System.console().readLine("Option: "));
            data += " " + new String(System.console().readLine("Please introduce the new value: "));
            out.writeUTF(data);
            System.out.print(in.readUTF());
        }
    }

    public static void createdirectory(String data) throws IOException, NullPointerException{
        String receive;
        String keywords[] = data.split(" ");
        
        if(error == 1){
            out.writeUTF(username + " " + password);
            in.readUTF();
            error = 0;
        }

        if(keywords.length != 2){
            out.writeUTF("ERROR");
            out.writeUTF(data);
            System.out.print(in.readUTF());
        }
        else{
            keywords = data.split(" ");
            
            out.writeUTF("CREATEDIRECTORY" + data.substring(5, data.length()));
            receive = in.readUTF();
            
            if(receive.split(" ")[0].compareTo("SUCCESS") == 0){
                System.out.print("Directory created\n\n");
            }
            else{                                
                System.out.print("Fails to create directory\n\n");
            }
        }
    }

    public static void changepassword(String data) throws IOException, NullPointerException{
        String receive;
        String keywords[] = data.split(" ");
        String aux_password;

        if(keywords.length != 1){
            out.writeUTF("ERROR");
            out.writeUTF(data);
            System.out.print(in.readUTF());
        }
        else{
            data = "CHANGEPASSWORD " +  new String(System.console().readPassword("Please introduce your old password: "));
            data += " " + new String(System.console().readPassword("Please introduce your new password: "));
            data += " " + (aux_password = new String(System.console().readPassword("Introduce your new password again: ")));
            out.writeUTF(data);
            receive = in.readUTF();
            System.out.print(receive);

            if(receive.compareTo("\nPassword sucessfully changed!\n\n") == 0){
                password = aux_password;
            }
        }
    }

    public static void listserverfiles(String data) throws IOException{
        String receive;
        String keywords[] = data.split(" ");

        if(error == 1){
            out.writeUTF(username + " " + password);
            in.readUTF();
            error = 0;
        }

        if(keywords.length != 1){
            out.writeUTF("ERROR");
            out.writeUTF(data);
            System.out.print(in.readUTF());
        }
        else{
            out.writeUTF("LISTFILES");
            receive = in.readUTF();
            keywords = receive.split(" ");

            if (receive.compareTo("ERROR") != 0){
                System.out.print("Files within the specified path:\n");
                for (int i = 1; i < keywords.length; i++){
                    if(i!=keywords.length-1){
                        System.out.print("   " + extendedAscii(204));
                    }
                    else{
                        System.out.print("   " + extendedAscii(200));
                    }

                    System.out.print(extendedAscii(205) + " " + keywords[i].substring(0, keywords[i].length() - 1));
                    
                    
                    if (keywords[i].endsWith("D")){
                        System.out.print(" - Directory\n");
                    }
                    else{
                        System.out.print(" - File\n");
                    }
                }

                System.out.print("\n");
            } 
            else{
                System.out.println("There are no files in the current folder!\n");
            }
        }
    }

    public static void changeserverdirectory(String data) throws IOException{
        String receive;
        String keywords[] = data.split(" ");
        
        if(error == 1){
            out.writeUTF(username + " " + password);
            in.readUTF();
            error = 0;
        }

        if(keywords.length == 1){
            out.writeUTF("ERROR");
            out.writeUTF(data);
            System.out.print(in.readUTF());
        }
        else{
            keywords = data.split(" ");
            
            out.writeUTF("CHANGEDIRECTORY" + data.substring(3, data.length()));
            receive = in.readUTF();
            
            if(receive.split(" ")[0].compareTo("SUCCESS") == 0){
                domain_server = receive.split(" |\n")[receive.split(" ").length - 1];
            }
            else{                                
                System.out.print(receive);
            }
        }
    }

    public static void listclientfiles() throws IOException{
        File folder = new File(client_path);
        File[] files = folder.listFiles();
        
        if (files == null || files.length == 0){
            System.out.println("\nThere are no files in the current folder!\n"); 
        }
        else{
            System.out.print("Files within the specified path:\n");
            for (int i = 0; i < files.length; i++){
                if(i != files.length-1){
                    System.out.print("   " + extendedAscii(204));
                }
                else{
                    System.out.print("   " + extendedAscii(200));
                }

                System.out.print(extendedAscii(205) + " " + files[i].getName().substring(0, files[i].getName().length()));
                
                
                if (files[i].isDirectory()){
                    System.out.print(" - Directory\n");
                }
                else{
                    System.out.print(" - File\n");
                }
            }
            
            System.out.print("\n");
        }    
    }

    public static void changeclientdirectory(String data){
        int returns;
        String keywords[] = data.split(" ");

        if(keywords.length != 1){
            returns = changedirectory(data.substring(keywords[0].length()+1, data.length()));
            if(returns == 0){
                System.out.print("The system cannot find the path specified\n\n");
            }
            else if(returns == -1){
                System.out.print("You don't have permission to navigate to here\n\n");
            }                              
        }
        else{
            System.out.print("'cd' is not recognized as an internal or external command\n\n");
        }
    }

    public static void downloadfiles(String data, String pathfail) throws NullPointerException, IOException, Exception{
        String [] keywords = data.split(" ");

        if(error == 1){
            out.writeUTF(username + " " + password);
            in.readUTF();
            error = 0;
        }

        if(keywords.length != 1){
            out.writeUTF("ERROR");
            out.writeUTF(data);
            System.out.print(in.readUTF());
        }
        else{
            data = new String(System.console().readLine("Please introduce the name of the file to download: "));
            out.writeUTF("DOWNLOAD " + data);

            if (in.readUTF().compareTo("AVAILABLE") == 0){
                new Download(data, client_path, pathfail);
                
                System.out.print("\n");
            } 
            else{
                System.out.print("\nFile not available in current server directory!\n\n");
            }
        }
    }

    public static void uploadfiles(String data, String pathfail) throws NullPointerException, IOException, Exception{
        String [] keywords = data.split(" ");
        File file;

        if(error == 1){
            out.writeUTF(username + " " + password);
            in.readUTF();
            error = 0;
        }

        if(keywords.length != 1){
            out.writeUTF("ERROR");
            out.writeUTF(data);
            System.out.print(in.readUTF());
        }
        else{
            data = new String(System.console().readLine("Please introduce the name of the file to upload: "));
            file = new File(client_path + "/" + data);

            if (file.exists()){
                out.writeUTF("UPLOAD");
                out.writeUTF(data);
                new Upload(data, client_path, pathfail);

                System.out.print("\n");
            } 
            else{
                System.out.println("\nThe required file doesn't exist in the current directory!\n");
            }
        }
    }
    
    public static void help(){
        System.out.print("\nHere's a list of available commands:\n");
        System.out.print("> changepass         Change Password\n> changeip           Change Server's Ip/Port\n");
        System.out.print("> config             Change IP/Port of Server\n");
        System.out.print("> mdkir              Create a directory in Server\n");
        System.out.print("> ls                 List Client's files and folders on present directory\n");
        System.out.print("> lsS                List Server's files and folders on present directory\n");
        System.out.print("> cd                 Change a local directory\n");
        System.out.print("> cdS                Change Server's directory\n");
        System.out.print("> downloadS          Download a file from the current directory\n");
        System.out.print("> uploadS            Upload a file to a current directory\n");
        System.out.print("> help               List available commands\n\n");
    }


    // AUXILIAR FUNCTIONS
    public static int changedirectory(String directory){
        String[] path = directory.split("/");
        String original_path = domain_client;
        int last_folder;
        
        if(directory.charAt(0) != '\\'){
            for(int i=0; i<path.length; i++){   
                if(path[i].equals("..")){
                    last_folder = original_path.lastIndexOf('\\');
                    if(last_folder == -1){
                        return -1;
                    }
                    else{
                        original_path = original_path.substring(0, last_folder);
                    }
                }   
                else{
                    if(!path[i].matches("[.]+")){
                        if(!directoryexits(original_path, path[i])){
                            return 0;
                        }               
                    
                        original_path += "\\" + path[i];
                    }    
                }
            }

            domain_client = original_path;
            client_path = System.getProperty("user.dir") + "\\" + domain_client;

            return 1;
        }

        return 0;
    } 

    public static boolean directoryexits(String actualdirectory, String directory){
        File folder = new File(client_path);
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

    public static int parseIntError(String possible_num){
        try{
            return Integer.parseInt(possible_num);
        }
        catch (NumberFormatException e){
            return 0;
        }
    }

    public static void filetransfer_error() throws IOException{
        s = new Socket("127.0.0.1", port);
        in = new DataInputStream(s.getInputStream());
        out = new DataOutputStream(s.getOutputStream());
        System.out.println("\nThere has been a connection error. Please input the desired command again!\n");
        
        error = 1;
    }

    public static char extendedAscii(int codePoint) throws UnsupportedEncodingException {
        return new String(new byte[] { (byte) codePoint }, "Cp437").charAt(0);
    }
}

class Upload extends Thread{
    private Socket sDU;
    private FileInputStream fin;
    private String name_file;
    private String client_path;
    private String pathfail;
    
    public Upload(String name_file, String client_path, String pathfail){
        this.name_file = name_file;
        this.client_path = client_path;
        this.pathfail = pathfail;
        this.start();
    }

    public void run(){
        int bytes;
        byte[] total_buffer = new byte[4*1024];
        DataOutputStream dout;
        File file = new File(client_path + "/" + name_file);

        try{
            fin = new FileInputStream(file);
            sDU = new Socket("127.0.0.1", 6001);
            dout = new DataOutputStream(sDU.getOutputStream());
            dout.writeLong(file.length());  
            
            while ((bytes=fin.read(total_buffer))!=-1){
                dout.write(total_buffer,0,bytes);
                dout.flush();
            }

            fin.close();
            sDU.close();
        }
        catch (IOException e){
            System.out.print("Failure while uploading a file! File might be corrupted!\n");
            System.out.print(pathfail);
        }
    }
}

class Download extends Thread{
    private Socket sDU;
    private FileOutputStream fout; 
    private String name_file;
    private String client_path;
    private String pathfail;

    public Download(String name_file, String client_path, String pathfail){
        this.name_file = name_file;
        this.client_path = client_path;
        this.pathfail = pathfail;
        this.start();
    }

    public void run(){
        int bytes = 0;
        byte[] total_buffer = new byte[4*1024];
        DataInputStream din;
        long size;

        try{
            fout = new FileOutputStream(client_path + "/" + name_file);
            sDU = new Socket("127.0.0.1", 6001);
            din = new DataInputStream(sDU.getInputStream());
            size = din.readLong();

            while (size > 0 && (bytes = din.read(total_buffer, 0, (int)Math.min(size, total_buffer.length))) != -1) {
                fout.write(total_buffer,0,bytes);
                size = size - bytes;
            }
            
            fout.close();
            sDU.close();
        }
        catch(IOException e){
            
            File file = new File(client_path + "/" + name_file);
            try{
                fout.close();
                try{
                    sDU.close();
                } 
                catch (NullPointerException f){
                    System.out.print("\nSocket wasn't created\n\n");
                    System.out.print(pathfail);
                }
                catch (IOException g){
                    System.out.print("\nError closing socket for download\n\n");
                    System.out.print(pathfail);
                }  
            } 
            catch (IOException f){
                System.out.print("\nError closing fileoutputstream for download\n\n");
                System.out.print(pathfail);
            }  

            if(file.delete()){
                System.out.print("\nFailure while downloading a file! Corrupted file has been deleted\n\n");
                System.out.print(pathfail);
            }
            else{
                System.out.print("\nFailure while downloading a file! Corrupted file cannot be deleted\n\n");
                System.out.print(pathfail);
            }
        }
    }
}
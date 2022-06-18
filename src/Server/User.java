
import java.io.Serializable;

public class User implements Serializable{
    private String username, password, department, address, phoneNumber, id, directory;
    private String idExpiration;

    public User(String username, String password, String department, String address, String phoneNumber, String id, String idExpiration, String directory){
        this.username = username;
        this.password = password;
        this.department = department;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.id = id;
        this.idExpiration = idExpiration;
        this.directory = directory;
    }

    //Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIdExpiration(String idExpiration) {
        this.idExpiration = idExpiration;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    //Getters
    public String getUsername(){
        return this.username;
    }

    public String getPassword(){
        return this.password;
    }

    public String getDepartment(){
        return this.department;
    }

    public String getAddress(){
        return this.address;
    }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public String getId(){
        return this.id;
    }

    public String getIdExpiration(){
        return this.idExpiration;
    }    

    public String getDirectory(){
        return this.directory;
    }

    public String toString(){
        return this.username + "#" + this.password + "#" + this.department + "#" + this.address + "#" + this.phoneNumber + "#" + this.id + "#" + this.idExpiration + "#" + this.directory; 
    }
}

# SD_Project-UcDrive
- [x] Finished
- [ ] Find where are the bugs
- [ ] Fix the bugs 

## Index
- [Description](#description)
- [Technologies used](#technologies-used)
- [To run this project](#to-run-this-project)
- [Notes important to read](#notes-important-to-read)
- [Authors](#authors)

## Description
This project was developed for Distributed Systems subject @University of Coimbra, Informatics Engineering <br>
Consists in develop a platform(server) to store files from users(clients), using some protocols (TCP and UDP) and other elements like sockets, packets, etc.

#### Main Languages:
![](https://img.shields.io/badge/Java-333333?style=flat&logo=java&logoColor=FFFFFF) 

## Technologies used:
1. Java
    - [Version ??](https://www.oracle.com/java/technologies/downloads/) 
2. RMI
    - You might have some problems here using environment variables

## To run this project:
[WARNING] Java must be installed<br>
You have two ways to run this project:
For both download the folder "#template"
1. Run the Executables
    * Download on folder "bin" the files *.jar
    * On folder "Server" and "SecondServer" put ucDrive.jar
    * On folder "Client" put the terminal.jar
    * The file console.jar can be wherever you want, like the folders "Server", "SecondServer", "Client", the program will continue working
    * Run each .jar using terminal in their directory
      ```shellscript
      [your-disk]:[name-path]> java -jar [file-name].jar
      ```

2. Running the code:
    * Download on folder "src" the files *.java and the bash file
    * On folder "Server" and "SecondServer" put the files: adm.java, Admin.java Server.java User.java
    * Compile the code in the directories of each server, this will create a folder Server with files *.class 
      ```shellscript
      [your-disk]:[name-path]> sh compile_server.sh
      ```
    * Run them 
      ```shellscript
      [your-disk]:[name-path]> java Server/Server
      ```
    * On folder "Client" put the file Client.java and run the code
      ```shellscript
      [your-disk]:[name-path]> java Client.java
      ```
    * To run an Admin User put adm.java and Admin.java in some folder and run it
      ```shellscript
      [your-disk]:[name-path]> java adm.java
      ```

## Notes important to read
- For more information about the project, commands and struct of resource files read the Report

## Authors:
- [João Silva](https://github.com/ikikara)
- [Mário Lemos](https://github.com/MrMarito) 
- [Pedro Martins](https://github.com/PedroMartinsUC) 

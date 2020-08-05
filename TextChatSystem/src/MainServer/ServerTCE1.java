/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author admin
 */
public class ServerTCE1 {

    //File
    private static final String SAVEFILE_ACOUNT = "Accounts.txt";
    //Connection
    private static final int PORT_NUM = 6969;
    //Resourses

    protected static Hashtable<String, ClientHandler> clientThreads;
    protected static Hashtable<String, user> allUsers;

    public static void main(String[] args) {
        //init
        clientThreads = new Hashtable<>();
        allUsers = new Hashtable<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(30);
        getResourses();
        
        if (!allUsers.isEmpty()) {
            System.out.println("--DONE_INIT--");
        } else {
            System.out.println("--INIT_Failed--");
        }

        System.out.println(allUsers.toString());
        
        try {
            ServerSocket ss = new ServerSocket(PORT_NUM);
            System.out.println("---SERVER_START---");
            while (true) {

                Socket s = ss.accept();
                
                executor.execute(new ClientHandler(s, clientThreads, allUsers));
//                System.out.println("\nConnection in " + s.getPort());
//                Thread sT = new serverThread(s);
//                sT.start();
            }
        } catch (IOException e) {
            System.out.println(e + "Init Failed");
        }

    }

    public static void getResourses() {
        try {
            allUsers = new Hashtable<>();
            try (RandomAccessFile f = new RandomAccessFile(SAVEFILE_ACOUNT, "r")) {
                String s;
                String[] a;

                while (true) {
                    s = f.readLine();
                    if (s == null || s.trim().equals("")) {
                        break;
                    }
                    System.out.println(s);

                    a = s.split("\\|\\%");
                    allUsers.put(a[0], new user(a[0], a[1], a[2]));
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

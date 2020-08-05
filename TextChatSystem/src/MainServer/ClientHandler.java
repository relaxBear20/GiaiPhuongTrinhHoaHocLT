/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author admin
 *
 * private final Socket cliSocket;
 */
public class ClientHandler implements Runnable {

    private static final String SAVEFILE_ACOUNT = "Accounts.txt";
    private final Socket cliSocket;
    protected static Hashtable<String, user> allUsers;
    //listener
    DataInputStream din;
    DataOutputStream dout;
    String inputString;
    Hashtable<String, ClientHandler> clientThreads;
    //Object Use
    String loginName = "";
    boolean active = false;

    Hashtable<String, String> offlineMess;

    public ClientHandler(Socket cliSocket, Hashtable<String, ClientHandler> clientThreads, Hashtable<String, user> allUsers) {
        this.clientThreads = clientThreads;
        this.cliSocket = cliSocket;
        this.allUsers = allUsers;
        System.out.println("****Connected client PORT " + cliSocket.getPort() + "****");
    }

    public String getLoginName() {
        return loginName;
    }

    void sendToClient(String resourse, String handler) {
        String tem = handler + "|%" + resourse;
        try {

            dout.writeUTF(tem);
        } catch (IOException e) {
        }
    }

    void handler(String resourse) {

        String[] tokens = inputString.split("\\|\\%");
        String mode = tokens[0];

        switch (mode) {

            case "0": {
                try {
                    if (!allUsers.containsKey(tokens[1])) {
                        addToFile(SAVEFILE_ACOUNT, tokens[1] + "|%" + tokens[2] + "|%" + tokens[3]);

                        allUsers.put(tokens[1], new user(tokens[1], tokens[2], tokens[3]));
                        sendToClient("", "/AddSuccess/");
                        clientThreads.forEach((t, u) -> {
                            if (!u.getLoginName().equals(this.getLoginName())) {
                                u.sendToClient(tokens[1], "/AddedAccount/");
                            }
                        });
                    } else {
                        dout.writeUTF("/AcountExisted/");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            }
            case "/CloseClient/": {

                stopConnect();

                return;

            }

            case "/Login/": {

                try {
                    if (allUsers.containsKey(tokens[1]) && allUsers.get(tokens[1]).getPass().equals(tokens[2])) {
                        if (clientThreads.containsKey(tokens[1])) {
                            sendToClient("", "/AlreadyLogined/");
                            return;
                        }
                        this.loginName = tokens[1];
                        clientThreads.put(this.loginName, this);

                        sendToClient("", "/LoginSuccess/");
                        active = true;
                        //get offlien and list
                        getOfflineMess();
                        getListOfOnlineUser();
                        //tell other client that this client is active
                        clientThreads.forEach((k, v) -> {
                            if (v != this) {
                                v.sendToClient(this.loginName, "/OtherGoesOnline/");
                            }
                        });
                    } else {
                        sendToClient("", "/LoginFailed/");
                        break;
                    }
                } catch (Exception e) {
                    sendToClient("", "/LoginFailed/");
                    System.out.println(e + " Login failed");
                }
                break;
            }

//            case "/GetListOfUsers/": {
//                getListOfOnlineUser();
//                clientThreads.forEach((k, v) -> {
//                    if (v != this) {
//                        v.sendToClient(this.loginName, "/OtherGoesOnline/");
//                    }
//                });
//                break;
//            }

            case "/InitChatTo/": {

                String receiverCli = tokens[1];
                if (clientThreads.containsKey(tokens[1])) {
                    ClientHandler receiverCliThread = clientThreads.get(receiverCli);

                    receiverCliThread.sendToClient(this.loginName, "/OpenChatFrom/");
                    sendToClient(receiverCli, "/OpenChatFrom/");
                    break;
                }
            }

            case "/MessSendTo/": {
                String content = tokens[2];
                ClientHandler receiverThread = clientThreads.get(tokens[1]);
                receiverThread.sendToClient(this.getLoginName() + "|%" + content, "/MessReceiFrom/");
                break;
            }
            case "/MessOfflineSendTo/": {
                String fname = tokens[2] + ".txt";
                String content = tokens[1] + "|%" + tokens[3];
                appendOfllineMess(fname, content);
                break;
            }

            case "/StopTo/": {
                String recieverClient = tokens[1];
                clientThreads.get(recieverClient).sendToClient(this.loginName, "/DropFrom/");
                break;
            }
            case "/overideOffFile/": {

                String fname = loginName + ".txt";
                String content = "";
                content = tokens[1];
                if (content.equals("-1")) {
                    content = "";
                } else {
                    content += "|%" + tokens[2];
                }

                System.out.println(fname + content);
                overideOfflineMess(fname, content);
                break;
            }

        }
    }

    public void overideOfflineMess(String fileName, String content) {
        try {
            FileWriter fw = new FileWriter(fileName, false);
            fw.write(content);
            fw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void appendOfllineMess(String fileName, String text) {
        try {
            File f = new File(fileName);
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            long fileLength = f.length();
            raf.seek(fileLength);
            raf.writeBytes(text + "\n");
            raf.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void getOfflineMess() {
        Hashtable<String, String> tempOfflineMess = new Hashtable<>();

        File f = new File(this.loginName + ".txt");
        if (!f.exists()) {
            return;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "r");
        } catch (FileNotFoundException e) {
            System.out.println("Cant get OfflineMESS " + e);
        }

        while (true) {
            String line = null;
            try {
                line = raf.readLine();
                System.out.println(line);
            } catch (IOException ex) {
                Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (line == null || line.trim().equals("")) {
                break;
            }

            String[] tem = line.split("\\|\\%");

            String name = tem[0];
            String content = tem[1];

            if (tempOfflineMess.containsKey(name)) {
                String preMess = tempOfflineMess.get(name);
                tempOfflineMess.put(name, preMess + "|$" + content);
            } else {
                System.out.println("put " +name + content);
                tempOfflineMess.put(name, content);
            }

        }
        tempOfflineMess.forEach((t, u) -> {
            sendToClient(t + "|%" + u, "/AddToOffline/");
        });

    }

    public void getListOfOnlineUser() {
        String online = "";
        String offline = "";
        String caller = this.loginName;
        Object[] allAcc = null;
        allAcc = allUsers.keySet().toArray();
        for (Object allAcc1 : allAcc) {
            String at = allAcc1.toString();
            if (clientThreads.containsKey(at) && !at.equals(caller)) {
                online += at + "|%";
            } else if (!at.equals(caller)) {
                offline += at + "|%";
            }
        }
        sendToClient(online, "/OtherOnlineClient/");
        sendToClient(offline, "/OtherOfflineClient/");
    }

    public void stopConnect() {
        try {
            if (active) {
                clientThreads.remove(this.loginName);
                clientThreads.forEach((k, v) -> {
                    v.sendToClient(this.loginName, "/OtherGoesOffline/");
                });

            }
            this.din.close();
            this.dout.close();

            this.cliSocket.close();
        } catch (IOException ext) {
            Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ext);
        }
    }

    @Override

    public void run() {

        try {

            din = new DataInputStream(cliSocket.getInputStream());
            dout = new DataOutputStream(cliSocket.getOutputStream());

        } catch (IOException e) {
            System.out.println("IO error " + this.getLoginName());
            return;
        }

        try {
            while (true) {

                //xu ly Request cua Client
                inputString = din.readUTF();

                if (loginName != null) {
                    System.out.println("-----Got request from " + this.loginName + "-----");
                }
                System.out.println(inputString);
                if (inputString.contains("/CloseClient/")) {
                    stopConnect();
                    break;
                }
                handler(inputString);
            }

        } catch (IOException ex) {

            Logger.getLogger(ServerTCE1.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("ERROR " + ex.toString());

            stopConnect();
        }

    }

    public static void addToFile(String fname, String content) {
        try {
            System.out.println("Printout to file " + content);
            File file = new File(fname);
            try (FileWriter fr = new FileWriter(file, true)) {
                fr.append(content + "\n");
                //fr.write(content);
            }
        } catch (IOException e) {
        }
    }

}

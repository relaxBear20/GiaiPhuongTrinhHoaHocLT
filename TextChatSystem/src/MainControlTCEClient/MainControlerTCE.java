/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainControlTCEClient;

import TCE_FORM_DIALOG.*;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.io.*;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

/**
 *
 * @author admin1
 */
public class MainControlerTCE {

    //For Connection 
    protected int PORT_NUM = 6969;
    DataInputStream din;
    DataOutputStream dout;
    boolean active = false;
    Socket s;
    String InputString;
    //For Client Dialog
    loginDialog logD;
    registerDialog regD;
    mainDialog mainD;
    Hashtable<String, chatDialog> onlineChatDialogses;
    Hashtable<String, chatDialog> offlineChatDialogses;
    Hashtable<String, Boolean> otherStt;
    //Client info
    protected String clientName;

    String[] listOfUsers;
    Hashtable<String, String> offlineMess;
    //Client management
    clientConnectionThread clientThread;

    public MainControlerTCE(loginDialog loginDialog, registerDialog registerDialog, mainDialog mainDialog) {
        this.logD = loginDialog;
        this.logD.setTitle("Login to TCS");
        this.regD = registerDialog;
        this.regD.setTitle("Register");
        this.mainD = mainDialog;
        this.mainD.setTitle("List of users");
        clientThread = new clientConnectionThread();
        this.onlineChatDialogses = new Hashtable<>();
        this.offlineChatDialogses = new Hashtable<>();
        this.offlineMess = new Hashtable<>();
        this.otherStt = new Hashtable<>();

        WindowAdapter wad;
        wad = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }

        };
        logD.addWindowListener(wad);
        mainD.addWindowListener(wad);
        regD.setDefaultCloseOperation(registerDialog.DISPOSE_ON_CLOSE);

    }

    public static void main(String[] args) {
        new MainControlerTCE(new loginDialog(), new registerDialog(), new mainDialog()).control();
    }

    void stop() {
        try {
            if (active) {
                if (offlineMess.isEmpty()) {
                    send("-1", "/overideOffFile/");
                } else {
                    String tem = "";
                    Object[] names = offlineMess.keySet().toArray();
                    for (Object name : names) {
                        tem += name + "|%" + offlineMess.get(name) + "\n";
                    }
                    send(tem, "/overideOffFile/");
                }
            }
            send("", "/CloseClient/");
            clientThread.stop();
            din.close();
            dout.close();
            s.close();

            System.exit(0);
        } catch (IOException ex) {
            Logger.getLogger(MainControlerTCE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void control() {
        clientThread.start();
        logD.setVisible(true);
        logBtnRegister();
        regBtnRegister();
        logBtnLogin();
        mainDChat();
        mainDBtnOff();
        mainDBtnClose();
        regBtnCancel();
        logCancel();

    }

    void send(String temp, String send) {
        try {
            //temp = temp.replaceAll("|%", "||");
            String output = send + "|%" + temp;
            System.out.println("Send " + output);
            dout.writeUTF(output);
        } catch (IOException ex) {
            System.out.println(ex + "Send to server failed");
            Logger.getLogger(MainControlerTCE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void handler(String input) {
        String[] tokens = input.split("\\|\\%");
        String mode = tokens[0];

        switch (mode) {
            case "/AcountExisted/": {
                JOptionPane.showMessageDialog(regD, "Account Name already Exited", "ERROR", 3);
                break;
            }
            case "/AddSuccess/": {
                regD.dispose();
                break;
            }
            case "/LoginFailed/": {
                JOptionPane.showMessageDialog(regD, "Login name or password is incorrect", "ERROR", 3);
                this.clientName = "";
                break;
            }
            case "/LoginSuccess/": {
                logD.dispose();
                active = true;
                mainD.getLblName().setText(clientName);
                mainD.setVisible(true);
                break;
            }
            case "/AddToOffline/": {
                String name = tokens[1];
                String content = tokens[2].replaceAll("\\[excep\\]", "|%");
                offlineMess.put(name, content);
                mainD.getBtnOfflineMessage().setText("Offline Messages (" + offlineMess.size() + ")");

            }
            case "/AddedAccount/": {
                String name = tokens[1];
                otherStt.put(name, Boolean.FALSE);
                updateMainList();
                break;
            }
            case "/OtherGoesOnline/": {
                otherStt.replace(tokens[1], true);
                updateMainList();
                break;
            }
            case "/OtherGoesOffline/": {
                otherStt.replace(tokens[1], false);
                updateMainList();
                break;
            }
            case "/OtherOnlineClient/": {

                for (int i = 1; i < tokens.length; i++) {
                    otherStt.put(tokens[i], true);
                }

                break;
            }
            case "/OtherOfflineClient/": {
                for (int i = 1; i < tokens.length; i++) {
                    otherStt.put(tokens[i], false);
                }
                updateMainList();

                break;
            }
            //Drop form other Client
            case "/DropFrom/": {
                try {
                    String caller = tokens[1];
                    if (!onlineChatDialogses.isEmpty()) {
                        chatDialog chatD = onlineChatDialogses.get(caller);
                        chatD.getTxtDisplay().setText(chatD.getTxtDisplay().getText() + "\n" + caller + " Has Disconnected");
                        chatD.setMode(false);
                        chatD.setTitle(chatD.getTitle().replaceFirst("Online", "Offline"));
                        offlineChatDialogses.put(caller, chatD);
                        onlineChatDialogses.remove(caller);
                    }
                    break;
                } catch (Exception e) {

                    System.out.println("DropForm " + e);
                }
            }

            case "/OpenChatFrom/": {
                if (offlineChatDialogses.containsKey(tokens[1])) {

                    chatDialog chatD = offlineChatDialogses.get(tokens[1]);
                    chatD.setMode(true);
                    chatD.setTitle(chatD.getTitle().replaceFirst("Offline", "Online"));
                    chatD.getTxtDisplay().setText(chatD.getTxtDisplay().getText() + "\n" + tokens[1] + " has join the chat");
                    offlineChatDialogses.remove(tokens[1]);
                    onlineChatDialogses.put(tokens[1], chatD);
                    break;

                } else {
                    createChatFrm(tokens[1], true);
                    break;
                }
            }

            case "/MessReceiFrom/": {
                String caller = tokens[1];
                String content = tokens[2].replaceAll("\\[excep\\]", "|%");
                chatDialog chatD = onlineChatDialogses.get(caller);
             
                chatD.getTxtDisplay().setText(chatD.getTxtDisplay().getText() + "\n" + caller + "\n   " + content);
                break;
            }

        }
    }

    void updateMainList() {
        Vector<String> v = new Vector<>();
        otherStt.forEach((k, vs) -> {
            if (vs) {
                v.add(k + " (online)");
            } else {
                v.add(k);
            }

        });
        DefaultComboBoxModel df = new DefaultComboBoxModel(v);
        mainD.getLstOnlineUsers().setModel(df);
        mainD.getLstOnlineUsers().setSelectedIndex(0);
    }

    void updateOffMess(offlineDialog offFrm) {
        DefaultComboBoxModel df = new DefaultComboBoxModel(offlineMess.keySet().toArray());
        offFrm.getLstDisplay().setModel(df);
        mainD.getBtnOfflineMessage().setText("Offline Messages (" + offlineMess.size() + ")");

    }

    void initOfflineView() {
        //set Up offFrm
        offlineDialog offFrm = new offlineDialog();
        offFrm.getLblWho().setText("login as: " + clientName);
        updateOffMess(offFrm);
        offFrm.setVisible(true);
        offFrm.getLstDisplay().setSelectedIndex(0);

//OffFram Btn read
        offFrm.getBtnRead().addActionListener((evt) -> {

            String name = offFrm.getLstDisplay().getSelectedValue();
            String content = this.offlineMess.get(name);
            StringTokenizer stt = new StringTokenizer(content, "$");
//MessSeen
            offlineMess.remove(name);
            updateOffMess(offFrm);

            String tem = "";
            while (stt.hasMoreTokens()) {
                tem += name + "\n   " + stt.nextToken() + "\n";
            }

            viewOfflineMess viewOff = new viewOfflineMess();
            viewOff.getLblWho().setText("From : " + name);
            viewOff.getTxtDisplay().setText(tem);
            viewOff.setVisible(true);
        });
    }

    void createChatFrm(String recevierClient, boolean mode) {
        //Create a Chat Frm
        chatDialog chatD = new chatDialog();
        chatD.setVisible(true);

        chatD.setCaller(this.clientName);
        chatD.setReceiver(recevierClient);
        String isOnline = mode == true ? "Online" : "Offline";
        chatD.setTitle("(" + isOnline + ") Chat with " + recevierClient);
        //mode
        chatD.setMode(mode);

        //init   
        chatD.getLblWhoToWho().setText(clientName + " chat to " + recevierClient);
        chatD.getTxtDisplay().setText("");
        //Close
        chatD.getBtnClose().addActionListener((e) -> {
            //Mode
            if (chatD.getMode()) {
                send(chatD.getReceiver(), "/StopTo/");
                onlineChatDialogses.remove(chatD.getReceiver());
                System.out.println(onlineChatDialogses.toString());
                chatD.dispose();
            } else {
                offlineChatDialogses.remove(chatD.getReceiver());
                chatD.dispose();
            }

        });
        chatD.setDefaultCloseOperation(chatDialog.DISPOSE_ON_CLOSE);
        //SendMessages
        chatD.getTxtINput().addKeyListener((new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String content = chatD.getTxtINput().getText();
                    if (chatD.getMode()) {
                        chatD.getTxtDisplay().setText(chatD.getTxtDisplay().getText() + "\n" + clientName + "\n   " + content);
                        if (chatD.getTxtINput().getText().isEmpty()) {
                            return;
                        }
                        
                        if(content.contains("|%")){
                            content = content.replaceAll("\\|\\%", "[excep]");
                        }
                        send(chatD.getReceiver() + "|%" + content, "/MessSendTo/");
                        chatD.getTxtINput().setText("");
                    } else {
                        chatD.getTxtDisplay().setText(chatD.getTxtDisplay().getText() + "\n" + clientName + "\n   " + chatD.getTxtINput().getText());
                        if (chatD.getTxtINput().getText().isEmpty()) {
                            return;
                        }
                        if(content.contains("|%")){
                            content = content.replaceAll("\\|\\%", "[excep]");
                        }
                        send(chatD.getCaller() + "|%" + chatD.getReceiver() + "|%" + content, "/MessOfflineSendTo/");
                        chatD.getTxtINput().setText("");
                    }
                }
            }
        }));

        onlineChatDialogses.put(recevierClient, chatD);

    }

    //main Stuff
    void mainDBtnClose() {
        mainD.getBtnClose().addActionListener((e) -> {
            stop();
            mainD.dispose();
        });
    }

    void mainDBtnOff() {
        mainD.getBtnOfflineMessage().addActionListener((e) -> {
            if (offlineMess.isEmpty()) {
                return;
            }
            initOfflineView();
        });
    }

    void mainDChat() {
        mainD.getBtnChat().addActionListener(((e) -> {
            String slt = mainD.getLstOnlineUsers().getSelectedValue();
            if (slt.contains("(online)")) {

                slt = slt.substring(0, slt.length() - 9);

            }
            if (otherStt.get(slt) && (!onlineChatDialogses.containsKey(slt))) {
                send(slt, "/InitChatTo/");
            } else if (!otherStt.get(slt) && !offlineChatDialogses.containsKey(slt)) {
                createChatFrm(slt, false);
            } else {
                JOptionPane.showConfirmDialog(mainD, "Already have a chat frame");
            }
        }));
    }

    //Login Stff
    void logBtnRegister() {
        logD.getBtnRegister().addActionListener((evt) -> {
            regD.getTxtName().setText("");
            regD.getTxtLogin().setText("");
            regD.getTxtPass().setText("");
            regD.getTxtRepass().setText("");
            regD.setVisible(true);
        });

    }

    void logBtnLogin() {
        logD.getBtnEnter().addActionListener((evt) -> {
            try {
                String login = logD.getTxtLogin().getText().trim();
                String pass = logD.getTxtPass().getText().trim();
                if (pass.equals("") || login.equals("")) {
                    JOptionPane.showConfirmDialog(logD, "Cant be Blank", "ERROR", 2);
                    return;
                }
                if (pass.matches("\\w*\\W+\\w*") || login.matches("\\w*\\W+\\w*")) {
                    JOptionPane.showConfirmDialog(logD, "Can contain non-words characters", "ERROR", 2);
                    return;
                }
                send(login + "|%" + pass, "/Login/");
                this.clientName = login;
            } catch (HeadlessException e) {
            }
        });
    }

    void logCancel() {
        logD.getBtnCancel().addActionListener((evt) -> {
            stop();
        });
    }

    //Register stff
    void regBtnCancel() {
        regD.getBtnCancel().addActionListener((e) -> {
            regD.dispose();
            regD.getTxtName().setText("");
            regD.getTxtLogin().setText("");
            regD.getTxtPass().setText("");
            regD.getTxtRepass().setText("");
        });
    }

    void regBtnRegister() {

        regD.getBtnOK().addActionListener((ActionEvent evt) -> {
            String login = "";
            String name = "";
            String pass = "";
            String temp = "";
            try {
                name = regD.getTxtName().getText().trim();
                name = name.replaceAll("\\s\\s", " ");
                login = regD.getTxtLogin().getText().trim();
                pass = regD.getTxtPass().getText().trim();
                if (pass.matches("(\\w*\\W+\\w*)") || name.matches("(\\w*\\W+\\w*)") || login.matches("(\\w*\\W+\\w*)")) {
                    JOptionPane.showMessageDialog(regD, "Cant contain non-word charcter(s)", "ERROR", 3);
                } else if (name.equals("") || login.equals("") || pass.equals("")) {
                    JOptionPane.showMessageDialog(regD, "Name and Login Can't not be blank", "ERROR", 3);

                } else if (!pass.equals(regD.getTxtRepass().getText())) {
                    JOptionPane.showMessageDialog(regD, "Re-type Password must be the same as password", "ERROR", 3);

                } else {
                    temp = login + "|%" + pass + "|%" + name;
                    send(temp, "0");
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(regD, "Can't not be blank", "ERROR", 3);
            }

        });
    }

    public class clientConnectionThread extends Thread {

        @Override
        public void run() {
            try {

                s = new Socket("localhost", PORT_NUM);
                din = new DataInputStream(s.getInputStream());
                dout = new DataOutputStream(s.getOutputStream());
            } catch (IOException e) {
                System.out.println("ERROR client can't Connect!");
                System.exit(0);
                System.out.println(e.toString());
            }
            while (true) {
                try {
                    InputString = din.readUTF();
                } catch (IOException ex) {

                    Logger.getLogger(MainControlerTCE.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Recived " + InputString);
                handler(InputString);
            }

        }

    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;

/**
 *
 * @author admin
 */
public class user {
    private String name;
    private String login;
    private String pass;

    public user() {
    }

    @Override
    public String toString() {
        return "user{" + "name=" + name + ", login=" + login + ", pass=" + pass + '}';
    }

    public user( String login, String pass,String name) {
        this.name = name;
        this.login = login;
        this.pass = pass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
    
}

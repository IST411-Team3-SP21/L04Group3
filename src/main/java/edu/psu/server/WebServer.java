package edu.psu.server;
/**
 * Project: Lab 4 Group Work
 * Purpose Details: HTTP Get and Post
 * Course: IST 411
 * Author: Team 3
 * Date Developed: 2/17/2020
 * Last Date Changed: 2/20/2020
 * Revision: 1
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {

    public WebServer() {
        System.out.println("Webserver Started");
        try (ServerSocket serverSocket = new ServerSocket(80)) {
            while (true) {
                System.out.println("Waiting for client request");
                Socket remote = serverSocket.accept();
                System.out.println("Connection made");
                new Thread(new ClientHandler(remote)).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) {
        new WebServer();
    }
}


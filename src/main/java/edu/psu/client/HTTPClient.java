package edu.psu.client;
/**
 * Project: Lab 4 Group Work
 * Purpose Details: HTTP Get and Post
 * Course: IST 411
 * Author: Team 3
 * Date Developed: 2/17/2020
 * Last Date Changed: 2/20/2020
 * Revision: 1
 */

import com.google.gson.Gson;
import edu.psu.server.entity.DiaryEntry;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Scanner;

public class HTTPClient {

    public HTTPClient() {
        System.out.println("HTTP Client Started");
        try {
            InetAddress serverInetAddress =
                    InetAddress.getByName("127.0.0.1");
            Socket connection = new Socket(serverInetAddress, 80);
            System.out.println("Connected to: " + serverInetAddress.getHostName());
            try (OutputStream out = connection.getOutputStream();
                 BufferedReader in =
                         new BufferedReader(new
                                 InputStreamReader(
                                 connection.getInputStream()))) {

                sendPost(out);
                System.out.println(getResponse(in));

                sendGet(out);
                System.out.println(getResponse(in));
                ;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * Gets input from a user (the post) and converts it into a POJO for serialization
     * into JSON.
     * @return return the JSON string
     */
    private String getDiaryPost() {
        System.out.print("Please Enter something to Post -> ");
        Scanner in = new Scanner(System.in);
        String entryTxt = in.nextLine();
        in.close();

        DiaryEntry diaryEntry = new DiaryEntry();
        diaryEntry.setBody(entryTxt);
        diaryEntry.setDate(LocalDate.now().toString());

        Gson gson = new Gson();
        return gson.toJson(diaryEntry);
    }

    /**
     * Sends our payload, as json, to the server. In a real life scenario,
     * where the server would actually be configured fully, we need to ensure our
     * meta data such as Content-Type is correct, so they can accept our message.
     * @param out {@link OutputStream} Socket output
     */
    private void sendPost(OutputStream out) {
        var json = getDiaryPost();
        try {
            out.write("POST /default\r\n".getBytes());
            out.write(String.format("Content-Length: %d\r\n", json.length()).getBytes());
            out.write("Content-Type: application/json\r\n".getBytes());
            out.write("User-Agent: Mozilla/5.0\r\n".getBytes());
            out.write(json.getBytes());
            System.out.println("Message sent to server.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends our get request to the server
     * @param out {@link OutputStream} Socket output
     */
    private void sendGet(OutputStream out) {
        try {
            out.write("GET /default\r\n".getBytes());
            out.write("User-Agent: Mozilla/5.0\r\n".getBytes());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * A Blocking call to wait for a response from the server
     * @param in {@link BufferedReader} The socket input
     * @return The response
     */
    private String getResponse(BufferedReader in) {
        try {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            return response.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) {
        new HTTPClient();
    }
}

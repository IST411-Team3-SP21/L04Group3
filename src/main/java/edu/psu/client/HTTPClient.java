package edu.psu.client;

import com.google.gson.Gson;
import edu.psu.server.entity.DiaryEntry;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.Scanner;

public class HTTPClient {

    public HTTPClient() {
        System.out.println("HTTP Client Started");
        try {
            InetAddress serverInetAddress =
                    InetAddress.getByName("127.0.0.1");
            Socket connection = new Socket(serverInetAddress, 80);

            try (OutputStream out = connection.getOutputStream();
                 BufferedReader in =
                         new BufferedReader(new
                                 InputStreamReader(
                                 connection.getInputStream()))) {
                sendPost(out);
                System.out.println(getResponse(in));
                sendGet(out);
                System.out.println(getResponse(in));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String getDiaryPost() {
        Scanner in = new Scanner(System.in);
        String entryTxt = in.nextLine();
        in.close();

        DiaryEntry diaryEntry = new DiaryEntry();
        diaryEntry.setBody(entryTxt);
        diaryEntry.setDate(LocalDate.now().toString());

        Gson gson = new Gson();
        return gson.toJson(diaryEntry);
    }

    private void sendPost(OutputStream out)  {
        var json = getDiaryPost();
        try {
            out.write("POST /default\r\n".getBytes());
            out.write(String.format("Content-Length: %d\r\n", json.length()).getBytes());
            out.write("Content-Type: application/json\r\n".getBytes());
            out.write("User-Agent: Mozilla/5.0\r\n".getBytes());
            out.write(json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendGet(OutputStream out) {
        try {
            out.write("GET /default\r\n".getBytes());
            out.write("User-Agent: Mozilla/5.0\r\n".getBytes());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

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

package edu.psu.server;

import com.google.gson.Gson;
import edu.psu.server.entity.DiaryEntries;
import edu.psu.server.entity.DiaryEntry;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    private final Socket socket;

    private Gson gson;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("\nClientHandler Started for " +
                this.socket);
        this.gson = new Gson();
        handleRequest(this.socket);
        System.out.println("ClientHandler Terminated for "
                + this.socket + "\n");
    }

    /**
     * Converts a input stream to string with a given encoding type
     *
     * @param inputStream input stream
     * @param encoding    The encoding type of the input
     * @return the input stream and a string
     * @throws UnsupportedEncodingException
     */
    private String jsonToString(InputStream inputStream, String encoding) throws UnsupportedEncodingException {
        return new BufferedReader(
                new InputStreamReader(inputStream, encoding))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    public DiaryEntries getDiaryPosts() throws Exception {
        File jarFile = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        String inputFilePath = jarFile.getParent() + File.separator + "diary.json";
        InputStream is = new FileInputStream("../diary.json");
        var fileBody = jsonToString(is, "UTF-8");
        return gson.fromJson(fileBody, DiaryEntries.class);
    }

    public void sendDiaryResponse(int code) {
        try {
            StringBuilder responseBuffer = new StringBuilder()
                    .append("<html><h1>Group3 WebServer Home Page.... </h1><br>")
                    .append("<b>Welcome to our wonderful web server!</b><BR>")
                    .append("<p>Here are my diary entries!</p>")
                    .append("<ul>");
            var diaryEntries = getDiaryPosts();
            for (DiaryEntry entry : diaryEntries.getDiaryEntries()) {
                responseBuffer.append(String.format("<li>User: %s - Date: %s: - Entry: %s  </li>",
                        entry.getIpAddress(),
                        entry.getDate(),
                        entry.getBody()));
            }
            responseBuffer.append("</ul>");

            System.out.println("Sending Response");
            sendResponse(socket, code, responseBuffer.toString());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            StringBuilder responseBuffer = new StringBuilder()
                    .append("<html><h1>Group3 WebServer Home Page.... </h1><br>")
                    .append("<b>Welcome to our wonderful web server!</b><BR>")
                    .append("<p>It looks like we had an error:</p>")
                    .append(String.format("<p>%s</p>", e.getMessage()))
                    .append("</html>");
            sendResponse(socket, code, responseBuffer.toString()); // To get it to display, we are using a 20x code, instead of 50x or 40x
        }
    }

    public void handleGET(StringTokenizer tokenizer) {
        System.out.println("Processing Get Request");
        String httpQueryString = tokenizer.nextToken();
        sendDiaryResponse(200);
    }

    public void createDiaryEntry(DiaryEntry diaryEntry) throws Exception {
        var diaryEntries = getDiaryPosts();
        diaryEntries.addDiaryEntry(diaryEntry);


        FileOutputStream fout = new FileOutputStream("../diary.json");
        fout.write(gson.toJson(diaryEntries).getBytes());
        fout.close();
    }

    private String parseJsonFromBody(String payload) {
        List<Character> stack = new ArrayList<>();
        List<String> json = new ArrayList<>();
        String tempStr = "";
        for (char nextChar : payload.toCharArray()) {
            if (stack.isEmpty() && nextChar == '{') {
                stack.add(nextChar);
                tempStr +=  nextChar;
            } else if (!stack.isEmpty()) {
                tempStr += nextChar;
                if (stack.get(stack.size() - 1).equals('{') && nextChar == '}') {
                    stack.remove(stack.size()-1);
                    if(stack.isEmpty()) {
                        json.add(tempStr);
                        tempStr += "";
                    }
                } else if (nextChar == '{' || nextChar == '}') {
                    stack.add(nextChar);
                }
            } else  if (tempStr.length() > 0 && stack.isEmpty()) {
                json.add(tempStr);
                tempStr = "";
            }
        }
        for (String nextJson: json) {
            System.out.println(nextJson);
        }

        return json.get(0);
    }

    public void handlePOST(BufferedReader in, StringTokenizer tokenizer) throws Exception {
        System.out.println("Processing POST Request");

        StringBuilder payload = new StringBuilder();
        while(in.ready()){
            payload.append((char) in.read());
        }
        var jsonStr = parseJsonFromBody(payload.toString());
        DiaryEntry diaryEntry = gson.fromJson(jsonStr, DiaryEntry.class);
        diaryEntry.setIpAddress(socket.getRemoteSocketAddress().toString());

        createDiaryEntry(diaryEntry);
        sendDiaryResponse(201);
    }


    public void handleRequest(Socket socket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            String headerLine = in.readLine();
            StringTokenizer tokenizer =
                    new StringTokenizer(headerLine);
            String httpMethod = tokenizer.nextToken();
            switch (httpMethod) {
                case "GET":
                    this.handleGET(tokenizer);
                    break;
                case "POST":
                    this.handlePOST(in, tokenizer);
                    break;
                default:
                    System.out.println("The HTTP method is not recognized");
                    sendResponse(socket, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendResponse(Socket socket,
                             int statusCode, String responseString) {
        String statusLine;
        String serverHeader = "Server: WebServer\r\n";
        String contentTypeHeader = "Content-Type: text/html\r\n";

        try (DataOutputStream out =
                     new DataOutputStream(socket.getOutputStream());) {
            if (statusCode == 200) {
                statusLine = "HTTP/1.0 200 OK" + "\r\n";
                String contentLengthHeader = "Content-Length: "
                        + responseString.length() + "\r\n";

                out.writeBytes(statusLine);
                out.writeBytes(serverHeader);
                out.writeBytes(contentTypeHeader);
                out.writeBytes(contentLengthHeader);
                out.writeBytes("\r\n");
                out.writeBytes(responseString);
            } else if (statusCode == 201) {
                statusLine = "HTTP/1.0 201 CREATED" + "\r\n";
                String contentLengthHeader = "Content-Length: "
                        + responseString.length() + "\r\n";

                out.writeBytes(statusLine);
                out.writeBytes(serverHeader);
                out.writeBytes(contentTypeHeader);
                out.writeBytes(contentLengthHeader);
                out.writeBytes("\r\n");
                out.writeBytes(responseString);
            } else if (statusCode == 405) {
                statusLine = "HTTP/1.0 405 Method Not Allowed" + "\r\n";
                out.writeBytes(statusLine);
                out.writeBytes("\r\n");
            } else {
                statusLine = "HTTP/1.0 404 Not Found" + "\r\n";
                out.writeBytes(statusLine);
                out.writeBytes("\r\n");
            }
            out.close();
        } catch (IOException ex) {
            // Handle exception
        }
    }


}


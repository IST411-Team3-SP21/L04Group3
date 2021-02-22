package edu.psu.server;

import com.google.gson.Gson;
import edu.psu.server.entity.DiaryEntries;
import edu.psu.server.entity.DiaryEntry;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * Reads in the diary entries from the json file, deserializes them into POJOs,
     * and returns the contents
     * @return {@link DiaryEntries}
     * @throws Exception Typically an IOException
     */
    public DiaryEntries getDiaryPosts() throws Exception {
        File file = new File("diary.json");
        file.createNewFile(); // will do nothing if already exists
        InputStream is = new FileInputStream("diary.json");
        var fileBody = jsonToString(is, "UTF-8");
        var entries = gson.fromJson(fileBody, DiaryEntries.class);
        if (entries == null) {
            return new DiaryEntries();
        }
        return entries;
    }

    /**
     * Sends all the diary responses as HTML, this is used for access within the browser
     * @param code Status Code
     */
    public void sendDiaryResponseHTML(int code) {
        try {
            StringBuilder responseBuffer = new StringBuilder()
                    .append("<html><h1>Group3 WebServer Home Page.... </h1><br>")
                    .append("<b>Welcome to our wonderful web server!</b><BR>")
                    .append("<p>Here are my diary entries!</p>");
            var diaryEntries = getDiaryPosts();

            if ( diaryEntries == null || diaryEntries.getDiaryEntries().isEmpty()) {
                responseBuffer.append("<p><strong>You have no entries!</strong></p>");
            } else {
                responseBuffer.append("<ul>");
                diaryEntries.getDiaryEntries().forEach(entry -> {
                    responseBuffer.append(String.format("<li>User: %s - Date: %s: - Entry: %s  </li>",
                            entry.getIpAddress(),
                            entry.getDate(),
                            entry.getBody()));
                });
                responseBuffer.append("</ul>");
            }

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

    /**
     * Get Handler (more like a controller to be honest) for incoming get requests
     * @param tokenizer
     */
    public void handleGET(StringTokenizer tokenizer) {
        System.out.println("Processing Get Request");
        String httpQueryString = tokenizer.nextToken();
        sendDiaryResponseHTML(200);
    }

    /**
     * A little lazy, but we essentially get all existing entries from the json file,
     * then we deserialize them, add the new entry, the serializer them back, and overwrite
     * the existing file, with the new structure of data.
     * @param diaryEntry {@link DiaryEntry} THe newly "created" diary entry
     * @throws Exception Typically an IOException
     */
    public void createDiaryEntry(DiaryEntry diaryEntry) throws Exception {
        var diaryEntries = getDiaryPosts();
        diaryEntries.addDiaryEntry(diaryEntry);

        File file = new File("diary.json");
        file.createNewFile(); // does nothing if already exists
        FileOutputStream fout = new FileOutputStream("diary.json");
        fout.write(gson.toJson(diaryEntries).getBytes());
        fout.close();
    }

    /**
     * Regex to parse out any JSON matches from the body of a POST request
     * @param payload Entire payload of POST request
     * @return
     */
    private String parseJsonFromBody(String payload) throws Exception {
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
        if (json.isEmpty()) throw new Exception("POST request has no body");
        return json.get(0);
    }

    /**
     * Handler for incoming POST requests
     * @param in {@link BufferedReader} input
     * @param tokenizer
     * @throws Exception
     */
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
        sendResponse(socket,201, gson.toJson(diaryEntry));
    }


    /**
     * Acts as the abstracted handler. Typically we would have some framework
     * like Spring, Play, etc.   provide an abstraction, that would route our requests, via a handler,
     * to the approriate controller, based on the route. But we are going really low level
     * here and making a sudo version of said handler.
     * @param socket {@link Socket}
     */
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
            System.err.println(e.getMessage());
        }
    }

    /**
     * Sends the appropriate response w/ body back to the client, based on the status code.
     * @param socket {@link Socket} The client socket
     * @param statusCode {@link Integer} For this we are really just using 20x, 40x
     * @param responseString The body of the response.
     */
    public void sendResponse(Socket socket, int statusCode, String responseString) {
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
            System.err.println(ex.getMessage());
        }
    }


}


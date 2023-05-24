package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;


public class CommunicationThread extends Thread {

    private final ServerThread serverThread;
    private final Socket socket;

    // Constructor of the thread, which takes a ServerThread and a Socket as parameters
    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    // run() method: The run method is the entry point for the thread when it starts executing.
    // It's responsible for reading data from the client, interacting with the server,
    // and sending a response back to the client.
    @Override
    public void run() {
        // It first checks whether the socket is null, and if so, it logs an error and returns.
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            // Create BufferedReader and PrintWriter instances for reading from and writing to the socket
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (pokemon name)!");

            // Read the city and informationType values sent by the client
            String pokemon = bufferedReader.readLine();
            if (pokemon == null || pokemon.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (pokemon name) !");
                return;
            }


            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
            HttpClient httpClient = new DefaultHttpClient();
            String pageSourceCode = "";

            // make the HTTP request to the web service
            HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS + "" + pokemon);
            HttpResponse httpGetResponse = httpClient.execute(httpGet);
            HttpEntity httpGetEntity = httpGetResponse.getEntity();
            if (httpGetEntity != null) {
                pageSourceCode = EntityUtils.toString(httpGetEntity);
            }
            if (pageSourceCode == null || pageSourceCode.toString().compareTo("Not Found") == 0) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                return;
            }

            // Parse the page source code into a JSONObject and extract the needed information
            JSONObject content = new JSONObject(pageSourceCode);
            JSONArray abilitiesArray = content.getJSONArray(Constants.ABILITIES);
            JSONObject abilityObj;
            StringBuilder abilitiesBuilder = new StringBuilder();

            for (int i = 0; i < abilitiesArray.length(); ++i) {
                abilityObj = abilitiesArray.getJSONObject(i).getJSONObject(Constants.ABILITY);

                abilitiesBuilder.append(abilityObj.getString(Constants.NAME)).append(" ,");
            }

            JSONArray typesArray = content.getJSONArray(Constants.TYPES);
            JSONObject typeObj;
            StringBuilder typeBuilder = new StringBuilder();

            for (int i = 0; i < typesArray.length(); ++i) {
                typeObj = typesArray.getJSONObject(i).getJSONObject(Constants.TYPE);

                typeBuilder.append(typeObj.getString(Constants.NAME)).append(" ,");
            }

            String url = content.getJSONObject(Constants.SPRITES).getString(Constants.IMAGE);

            if (url == null || typeBuilder.toString().length() < 1 || abilitiesBuilder.toString().length() < 1) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice! Information is empty");
                return;
            }

            // Send the result back to the client
            printWriter.println(abilitiesBuilder.toString());
            printWriter.flush();
            printWriter.println(typeBuilder.toString());
            printWriter.flush();
            printWriter.println(url);
            printWriter.flush();
        } catch (IOException | JSONException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } finally {
            try {
                socket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

}

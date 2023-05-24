package ro.pub.cs.systems.eim.practicaltest02;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;


public class ClientThread extends Thread {

    private final String address;
    private final int port;
    private final String pokemon;
    private final TextView pokemonTypes;
    private final TextView pokemonAbilities;
    private final ImageView pokemonImage;

    private Socket socket;

    public ClientThread(String address, int port, String pokemon, TextView pokemonTypes, TextView pokemonAbilities, ImageView pokemonImage) {
        this.address = address;
        this.port = port;
        this.pokemon = pokemon;
        this.pokemonTypes = pokemonTypes;
        this.pokemonAbilities = pokemonAbilities;
        this.pokemonImage = pokemonImage;
    }

    @Override
    public void run() {
        try {
            // tries to establish a socket connection to the server
            socket = new Socket(address, port);

            // gets the reader and writer for the socket
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);

            // sends the city and information type to the server
            printWriter.println(pokemon);
            printWriter.flush();
            String abilities, types, url;

            // reads the weather information from the server
//            while ((weatherInformation = bufferedReader.readLine()) != null) {
//                final String finalizedWeateherInformation = weatherInformation;
//
//                // updates the UI with the weather information. This is done using postt() method to ensure it is executed on UI thread
//                weatherForecastTextView.post(() -> weatherForecastTextView.setText(finalizedWeateherInformation));
//            }

            abilities = bufferedReader.readLine();
            types = bufferedReader.readLine();
            url = bufferedReader.readLine();

            System.out.println(abilities);
            System.out.println(types);
            System.out.println(url);


            pokemonTypes.post(() -> pokemonTypes.setText(types));
            pokemonAbilities.post(() -> pokemonAbilities.setText(abilities));
            pokemonImage.post(() -> Picasso.get().load(url).into(pokemonImage));
        } // if an exception occurs, it is logged
        catch (IOException ioException) {
            Log.e(Constants.TAG, "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (socket != null) {
                try {
                    // closes the socket regardless of errors or not
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

    static Bitmap downloadBitmap(String url) {
        HttpClient client = new DefaultHttpClient();
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            // Could provide a more explicit error message for IOException or IllegalStateException
            getRequest.abort();
            Log.e(Constants.TAG, "Cannot get the image");
        }
        return null;
    }

}

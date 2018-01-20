package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT[] = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private final Uri mUri;

    public GroupMessengerActivity() {
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button4 = (Button) findViewById(R.id.button4);
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            ContentValues mContentValues = new ContentValues();
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            String recvdMsg;
            int sequence_no = 0;
            try {
                while (true) {
                    Socket server = serverSocket.accept();
                    Log.e(TAG, "Connection Accepted by server");
                    BufferedReader in =
                            new BufferedReader(new InputStreamReader(server.getInputStream()));
                    PrintWriter outs =
                            new PrintWriter(server.getOutputStream(), true);
                    recvdMsg = in.readLine();
                    Log.e(TAG, "Received Message :" + recvdMsg);
                    outs.print("PA-2 OK");
                    mContentValues.put(KEY_FIELD, Integer.toString(sequence_no));
                    mContentValues.put(VALUE_FIELD, recvdMsg);
                    getContentResolver().insert(mUri, mContentValues);
                    publishProgress(recvdMsg);
                    outs.close();
                    in.close();
                    server.close();
                    sequence_no++;
                    mContentValues.clear();
                    if (recvdMsg.equals("bye")) {
                        break;
                    }
                }
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Can't create a listening socket for server");
            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            for(int i = 0; i < 5; i++){
                try {
                    String remotePort = REMOTE_PORT[i];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    String msgToSend = msgs[0];
                    Log.e(TAG, "Sending the message to server : " + msgToSend);
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader inc =
                            new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out.println(msgToSend);
                    Log.e(TAG, "Message sent : " + msgToSend);
                    String serverAck;
                    if ((serverAck = inc.readLine()) != null)
                        while (!serverAck.equals("PA-2 OK"))
                            inc.close();
                    out.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }


            return null;
        }
    }
}

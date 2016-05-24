package edu.buffalo.cse.cse486586.groupmessenger2;

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
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    Boolean REMOTE_PORT0_B = true;
    Boolean REMOTE_PORT1_B = true;
    Boolean REMOTE_PORT2_B = true;
    Boolean REMOTE_PORT3_B = true;
    Boolean REMOTE_PORT4_B = true;
    static final int SERVER_PORT = 10000;
    static final String DOT = ".";
    static int sequenceNumber = 0;
    int maxSequenceNumber = 0;
    int sequencer = 1;
    private String thisPort = null;
    int insertKeySeqNum = 0;
    long counter1 = 0;

    Hashtable<String,String> messagesFromSender= new Hashtable<String,String>();
    Hashtable<String,Integer> proposalTracker= new Hashtable<String, Integer>();
    Hashtable<String, ArrayList<Double>> proposalList= new Hashtable<String,ArrayList<Double>>();
    Hashtable<String,String> receivedMsgList= new Hashtable<String, String>();
    Hashtable<String,String> receiverProposalList = new Hashtable<String, String>();
    PriorityBlockingQueue<String> holdBackQueue= new PriorityBlockingQueue<String>(5);
    PriorityBlockingQueue<String> deliveryQueue= new PriorityBlockingQueue<String>(5);
    PriorityBlockingQueue<String> finalQueue= new PriorityBlockingQueue<String>(25);
    PriorityBlockingQueue<String> messageQueue= new PriorityBlockingQueue<String>(25, new Comparator<String>() {
        @Override
        public int compare(String first, String second) {
            if (Integer.parseInt(first.split(":")[1]) > Integer.parseInt(second.split(":")[1])) {
                return 1;
            } else if (Integer.parseInt(first.split(":")[1]) < Integer.parseInt(second.split(":")[1])) {
                return -1;
            } else if (Integer.parseInt(first.split(":")[2]) > Integer.parseInt(second.split(":")[2])) {
                return 1;
            } else if (Integer.parseInt(first.split(":")[2]) < Integer.parseInt(second.split(":")[2])) {
                return -1;
            }
            return 0;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        thisPort = myPort;
        final EditText editText = (EditText) findViewById(R.id.editText1);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (Exception e){
            Log.e(TAG, "Server Socket connection cannot be established");
        }

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            /**
             * Called when a view has been clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                counter1++;
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort, String.valueOf(counter1));
                String counter2 = myPort+":"+counter1;
                messagesFromSender.put(counter2, msg);
                proposalTracker.put(counter2,0);
                proposalList.put(counter2, new ArrayList<Double>());

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            try{

                while(true){
                    Socket serverSocketConnection = serverSocket.accept();
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(serverSocketConnection.getInputStream()));
                    //Publish the message on the UI
                    String p = in.readLine();
                    Log.v(TAG, "Message received:" + p);
                    String [] partsOfMessageReceived = p.split(":");

                    if(partsOfMessageReceived[0].compareToIgnoreCase("NEWMESSAGE")==0){
                        receivedMsgList.put(partsOfMessageReceived[1]+":"+partsOfMessageReceived[2], partsOfMessageReceived[3]);
                        callClientTask("NEWMESSAGE" + ":" + partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2]);

                    }
                    else if(partsOfMessageReceived[0].compareToIgnoreCase("PRIORITY")==0){
                        int counter3;
                        Log.v(TAG,"tempProposalList1:"+partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2]);
                        Log.v(TAG,"tempProposalList2:"+proposalTracker.get(partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2]));
                        ArrayList<Double> maxList = proposalList.get(partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2]);
                        if((counter3= proposalTracker.get(partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2]) + 1) < 5){
                            maxList.add(Double.parseDouble(partsOfMessageReceived[3]));
                            proposalList.put(partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2],maxList);
                            proposalTracker.put(partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2],counter3);
                            Log.v(TAG, "trackerCount:" + counter3);
                        }else{
                            Double agreedSeq = Collections.max(maxList);
                            callClientTask("PRIORITY" + ":" + partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2] + ":" + String.valueOf(agreedSeq));
                        }

                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("MESSAGEWITHPRIORITY")==0){
                        String msgData=receivedMsgList.get(partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2]);
                        if(msgData!=null && partsOfMessageReceived[3]!=null) {
                            holdBackQueue.remove(receiverProposalList.get(partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2]));
                            deliveryQueue.add(partsOfMessageReceived[3] + ":" + partsOfMessageReceived[1]+":"+partsOfMessageReceived[2] + msgData + "TRUE");

                            String heldBackMsg = holdBackQueue.peek();
                            String message = deliveryQueue.peek();
                            if (heldBackMsg != null) {
                                while (true) {
                                    if (message != null && Double.parseDouble(heldBackMsg.split(":")[0]) > Double.parseDouble(message.split(":")[0])) {
                                        finalQueue.add(message);
                                        deliveryQueue.poll();
                                        message = deliveryQueue.peek();
                                    } else
                                        break;
                                }
                            } else {
                                while (true) {
                                    if (message != null) {
                                        finalQueue.add(message);
                                        deliveryQueue.poll();
                                        message = deliveryQueue.peek();
                                    } else
                                        break;
                                }
                            }
                        }
                    }
                }


            }catch(Exception exception){
                Log.e(TAG,"IO Exception in server socket connection accept()");
            }
            return null;


        }


        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(strReceived + "\t\n");
            localTextView.append("\n");

            ContentValues cv = new ContentValues();
            cv.put("key",sequenceNumber);
            sequenceNumber = sequenceNumber + 1;
            cv.put("value", strReceived);
            getContentResolver().insert(new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.groupmessenger2.provider").build(),cv);
            return;
        }

        protected void callClientTask(String message){
            Log.v(TAG,"callClientTask:"+message);
            if(message!=null) {
                String[] partsOfMessageReceived = message.split(":");
                if(partsOfMessageReceived[0].compareToIgnoreCase("NEWMESSAGE") == 0){
                    new ProposalClientTask().execute(partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2]);
                }
                else if(partsOfMessageReceived[0].compareToIgnoreCase("PRIORITY")==0){
                    new AgreementClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, partsOfMessageReceived[1] + ":" + partsOfMessageReceived[2], partsOfMessageReceived[3]);
                }
            }
        }
    }

    private class ProposalClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            Log.v(TAG,"ProposalClientTask:"+msgs[0]);
            try {
                String strSeqNumToSend;
                int seqNumToSend;

                String[] tempStr = msgs[0].split(":");
                String senderPort = tempStr[0];
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(senderPort));
                socket.setSoTimeout(500);
                DataOutputStream writeOutputStream = new DataOutputStream(socket.getOutputStream());

                if (sequencer > maxSequenceNumber){
                    seqNumToSend = sequencer;
                }else{
                    seqNumToSend = maxSequenceNumber;
                }
                sequencer = seqNumToSend + 1;

                if (!thisPort.equals(null)) {
                    strSeqNumToSend = seqNumToSend + DOT + thisPort;
                } else{
                    strSeqNumToSend = seqNumToSend + DOT + "0";
                }

                String newMessage = strSeqNumToSend + ":" + msgs[0] + ":" + receivedMsgList.get(msgs[0]) + ":" + "FALSE";

                holdBackQueue.add(newMessage);
                messageQueue.add(newMessage);
                PriorityBlockingQueue<String> copyMessageQueue = new PriorityBlockingQueue<String>(messageQueue);
                insertKeySeqNum=0;
                for(String tempMsg; (tempMsg = copyMessageQueue.poll())!=null;){
                    String key = Integer.toString(insertKeySeqNum++);
                    ContentValues cv = new ContentValues();
                    cv.put("key",key);
                    cv.put("value",tempMsg.split(":")[3]);
                    getContentResolver().insert(new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.groupmessenger2.provider").build(), cv);
                    Log.v(TAG, "Key inserted:" + key);
                    Log.v(TAG, "Value inserted:" + tempMsg.split(":")[3]);
                }
                receiverProposalList.put(msgs[0], newMessage);
                String msgToSend = "PRIORITY" + ":" + msgs[0] + ":" + strSeqNumToSend + ":";
                writeOutputStream.write(msgToSend.getBytes());
                socket.close();
                Log.v(TAG,"Message sent from proposal client:"+msgToSend);
            } catch (Exception e) {
                Log.e(TAG, "Exception from Proposal Client Task ");
            }
            return null;
        }
    }

    private class AgreementClientTask extends AsyncTask<String, Void, Void> {

            @Override
            protected Void doInBackground(String... msgs) {
                String msgToSend = "MESSAGEWITHPRIORITY" + ":" + msgs[0] + ":" + msgs[1] + ":";
                Log.v(TAG,"msgToSend in AgreementClientTask:"+msgToSend);

                try {
                    if (REMOTE_PORT0_B) {
                        Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT0));
                        socket0.setSoTimeout(500);
                        socket0.getOutputStream().write(msgToSend.getBytes());
                        socket0.close();
                        Log.v(TAG, "Message sent from REMOTE_PORT0:" + REMOTE_PORT0_B);
                    }

                    if (REMOTE_PORT1_B) {
                        Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT1));
                        socket1.setSoTimeout(500);
                        socket1.getOutputStream().write(msgToSend.getBytes());
                        socket1.close();
                        Log.v(TAG, "Message sent from REMOTE_PORT1:" + REMOTE_PORT1_B);
                    }

                    if (REMOTE_PORT2_B) {
                        Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT2));
                        socket2.setSoTimeout(500);
                        socket2.getOutputStream().write(msgToSend.getBytes());
                        socket2.close();
                        Log.v(TAG, "Message sent from REMOTE_PORT2:" + REMOTE_PORT2_B);
                    }

                    if (REMOTE_PORT3_B) {
                        Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT3));
                        socket3.setSoTimeout(500);
                        socket3.getOutputStream().write(msgToSend.getBytes());
                        socket3.close();
                        Log.v(TAG, "Message sent from REMOTE_PORT3:" + REMOTE_PORT3_B);
                    }

                    if (REMOTE_PORT4_B) {
                        Socket socket4 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT4));
                        socket4.setSoTimeout(500);
                        socket4.getOutputStream().write(msgToSend.getBytes());
                        socket4.close();
                        Log.v(TAG, "Message sent from REMOTE_PORT4:" + REMOTE_PORT4_B);
                    }

                    Log.e(TAG, "Sending message:" + msgToSend);

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
                return null;
            }
        }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            try {

                String msgToSend = "NEWMESSAGE" + ":" + msgs[1]+ ":" + msgs[2] + ":" + msgs[0] +":";

                Log.v(TAG,"Message to send:"+msgToSend);

                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT0));
                    socket0.setSoTimeout(500);
                    socket0.getOutputStream().write(msgToSend.getBytes());
                    socket0.close();
                Log.v(TAG, "Message sent to AVD0:"+msgToSend);

                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT1));
                    socket1.setSoTimeout(500);
                    socket1.getOutputStream().write(msgToSend.getBytes());
                    socket1.close();
                Log.v(TAG, "Message sent to AVD1:"+msgToSend);

                    Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT2));
                    socket2.setSoTimeout(500);
                    socket2.getOutputStream().write(msgToSend.getBytes());
                    socket2.close();
                Log.v(TAG, "Message sent to AVD2:"+msgToSend);

                    Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT3));
                    socket3.setSoTimeout(500);
                    socket3.getOutputStream().write(msgToSend.getBytes());
                    socket3.close();
                Log.v(TAG, "Message sent to AVD3:"+msgToSend);

                    Socket socket4 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT4));
                    socket4.setSoTimeout(500);
                    socket4.getOutputStream().write(msgToSend.getBytes());
                    socket4.close();
                Log.v(TAG, "Message sent to AVD4:"+msgToSend);

                    Log.e(TAG, "Sending message:"+msgToSend);

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
            return null;
        }
    }
}

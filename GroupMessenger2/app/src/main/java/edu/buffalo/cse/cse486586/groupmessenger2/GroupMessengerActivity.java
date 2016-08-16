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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static private final  ArrayList<String> REMOTE_PORT = new ArrayList<String>();
    //static private String[] REMOTE_PORT={"11108","11112","11116","11120","11124"};
    //String[] arr = {"11108","11112"};

    static final int SERVER_PORT = 10000;

    private ArrayList<Message> pqueue = new ArrayList<Message>();


    private Lock lock;

    private int proposed = 1;
    //    static private int aliveNodes = 5;
    //
    private int msgCount = 1;
    static private int count = 0;
    private int n = 0;


    static private ArrayList<String> copy_first = new ArrayList<String>();
    static private ArrayList<String> copy_second = new ArrayList<String>();

    public static Comparator<Message> priorityComparator = new Comparator<Message>() {
        public int compare(Message m1,Message m2)
        {
            if (m1.p == m2.p)
                return m1.PNum-m2.PNum;
            else
                return m1.p-m2.p;

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        System.out.println("***** IN ONCREATE");
        REMOTE_PORT.add("11108");
        REMOTE_PORT.add("11112");
        REMOTE_PORT.add("11116");
        REMOTE_PORT.add("11120");
        REMOTE_PORT.add("11124");


        for (int i=0 ;i<5;i++) {

            if (Integer.parseInt(myPort) == Integer.parseInt(REMOTE_PORT.get(i)) )
            {
                n = i;
            }
        }
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        final EditText editText = (EditText) findViewById(R.id.editText1);
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        /*
         * registers and implements an OnClickListener for the "Send" button.
         * gets the message from the input box (EditText)
         * and sends it to other AVDs.
         */
        Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = editText.getText().toString() + "\n";
                TextView tv1 = (TextView) findViewById(R.id.textView1);
                editText.setText("");
                tv1.append("\t" + msg);
                msg = "M:"+ proposed +":"+n+":"+ msgCount +":"+n+":false:"+ msg; // type P Pnum Msgid sent_by deliverable text(msg)
                msgCount++;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
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
        protected Void doInBackground(ServerSocket... sockets)
        {
            ServerSocket serverSocket = sockets[0];
            Socket clientSocket = null;
            BufferedReader inMsg;
            try {

                while(true)
                {
                    clientSocket = serverSocket.accept();

                    clientSocket.setSoTimeout(500);


                    inMsg = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String Msg = inMsg.readLine();

                    String [] SplitParts = Msg.split(":");
                    Message incomingMsg = new Message();

                    incomingMsg.type = SplitParts[0];
                    incomingMsg.p = Integer.parseInt(SplitParts[1]);
                    incomingMsg.PNum = Integer.parseInt(SplitParts[2]);
                    incomingMsg.msgId = Integer.parseInt(SplitParts[3]);
                    incomingMsg.seen_by = Integer.parseInt(SplitParts[4]);

                    if(SplitParts[5].equals("false"))
                        incomingMsg.deliverable = false;
                    else
                        incomingMsg.deliverable=true;

                    incomingMsg.msg = SplitParts[6];

                    if(incomingMsg.type.equals("M"))
                    {

                        //add msg to queue & send back the proposed priority
                        incomingMsg.type = "P";
                        proposed++;
                        incomingMsg.p = proposed;
                        // 17 March 11:45 PM
                        //
                        //  if (incomingMsg.seen_by != n)
                        //{
                        //  incomingMsg.seen_by = n;
                            /*for(Message m:pqueue)
                            {
                             /// CHECK FOR MSG TAG AND SENDER TAG. REST OF THE TAGS WILL OBVIOUSLY CHANGE OVER TIME!

                                if(m.sent_by == incomingMsg.sent_by && m.msg==incomingMsg.msg){}
                            else {*/
                        pqueue.add(incomingMsg);
                        //}
                        //}

                        //}


                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        String proposed_msg=incomingMsg.type+":"+incomingMsg.p+":"+incomingMsg.PNum+":"+incomingMsg.msgId+":"+incomingMsg.sent_by+":"+incomingMsg.deliverable+":"+incomingMsg.msg;

                        out.println(proposed_msg + "\r\n");

                    }


                    else if (incomingMsg.type.equals("R"))
                    {

                        //check queue for the msg and update its priority with agreed.

                        proposed = (incomingMsg.p > proposed)? incomingMsg.p:proposed;

                        for(Message qMsg : pqueue)
                        {
                            if ((qMsg.msgId == incomingMsg.msgId) && (qMsg.PNum == incomingMsg.PNum))
                            {

                                // 17 March
                                //if(REMOTE_PORT.contains(qMsg.sent_by)) {
                                qMsg.p = incomingMsg.p;
                                qMsg.deliverable = true;

                                break;

                            }
                        }

                        Collections.sort(pqueue, priorityComparator);

                        count=0;

                        Iterator<Message> iter = pqueue.iterator();
                        //System.out.println("PRINTING QUEUE CONTENTS : "+pqueue.size());
                        for(Message message:pqueue)
                        {
                            if(message.deliverable==true) {
                          //      System.out.println(" " + message.msg);
                                //publishProgress(message.msg);

                                if(count<25) {
                                    //insert into db
                                    String key = Integer.toString(count);
                                    ContentValues values = new ContentValues();
                                    values.put("key", key);
                                    values.put("value", message.msg);


                                    Uri.Builder uriBuilder = new Uri.Builder();
                                    uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
                                    uriBuilder.scheme("content");
                                    Uri iUri = uriBuilder.build();
                                    Uri newUri = getApplicationContext().getContentResolver().insert(iUri, values);


                                    count++;
                                }



                            }
                        }




// 17 March                        clientSocket.close();

                    } // END OF ELSE IF MSG TYPE= R

                } // END OF WHILE (TRUE)

/*

                    Iterator<Message> iter = pqueue.iterator();
                    while (iter.hasNext())
                    {
                        Message checkmsg = iter.next();

                        //17 MARCH
                    /*    if(!REMOTE_PORT.contains(checkmsg.sent_by)) // IF THE MSG WHICH IS ALREADY IN QUEUE HAS BEEN SENT BY SENDER WHICH HAS DIED NOW, REMOVE IT FROM QUEUE
                        {
                            iter.remove();
                        }


                        if (checkmsg.deliverable )
                        {

//                            for(Message m: pqueue){
                            publishProgress(checkmsg.msg);
                        //}
                             // inseriing whole q uptil now &&  q shud not have duplicates
                            // 17 March 11:30 PM iter.remove();
                        }
                        else
                        {
                            break;
                        }

                    }
*/







            } // END OF TRY BLOCK


            catch (Exception e) {

                Log.e(TAG, "Exception in Server Task");
            }




            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");


            //insert into db
            String key = Integer.toString(count);
            ContentValues values = new ContentValues();
            values.put("key",key);
            values.put("value",strReceived);


            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            uriBuilder.scheme("content");
            Uri iUri = uriBuilder.build();
            Uri newUri = getApplicationContext().getContentResolver().insert(iUri,values);


            count++;
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String msgToSend = msgs[0];
            Message sendingObj = new Message();

            String[] Parts = msgToSend.split(":");
            sendingObj.type = Parts[0];
            sendingObj.p = Integer.parseInt(Parts[1]);
            sendingObj.PNum = Integer.parseInt(Parts[2]);
            sendingObj.msgId = Integer.parseInt(Parts[3]);
            sendingObj.sent_by = Integer.parseInt(Parts[4]);

            if(Parts[5].equals("false"))
                sendingObj.deliverable = false;
            else
                sendingObj.deliverable=true;

            sendingObj.msg = Parts[6];


            /*for(Message mess:pqueue) { // ONLY UNIQUE INSERTIONS ALLOWED IN QUEUE

                if(mess.msg == sendingObj.msg && mess.sent_by == sendingObj.sent_by)
                {

                }
                else {*/
            // pqueue.add(sendingObj);
            //}
            //}

            // lock.lock();
//
            for (int j = 0; j < REMOTE_PORT.size(); j++) {

                //lock.unlock();
                Socket socket = null;
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT.get(j)));

                    socket.setSoTimeout(1000);

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(msgToSend + "\r\n");
                    //                   socket.close();
//-------------------------------------------------------------------------


                    //READING PROPOSED PRIORITY MESSAGE

                    BufferedReader iMsgStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //socket.setSoTimeout(500);
                    String iMsg = iMsgStream.readLine();

                    String [] iMsgParts = iMsg.split(":");
                    Message incomingMsg_proposed = new Message();

                    sendingObj.type = iMsgParts[0];
                    sendingObj.p = Integer.parseInt(iMsgParts[1]);
                    sendingObj.PNum = Integer.parseInt(iMsgParts[2]);
                    sendingObj.msgId = Integer.parseInt(iMsgParts[3]);
                    sendingObj.seen_by = Integer.parseInt(iMsgParts[4]);

                    if(iMsgParts[5].equals("false"))
                        sendingObj.deliverable = false;
                    else
                        sendingObj.deliverable=true;

                    incomingMsg_proposed.msg = iMsgParts[6];

                    //if (incomingMsg_proposed.type.equals("P")) {

                    for(Message qMsg : pqueue)
                    {

                        if ((sendingObj.msgId == incomingMsg_proposed.msgId) && (sendingObj.PNum == incomingMsg_proposed.PNum))
                        {
                            sendingObj.seen_by += 1;

                            sendingObj.p = (sendingObj.p > incomingMsg_proposed.p) ? sendingObj.p : incomingMsg_proposed.p;

                            break;
                        }
                    }

                    socket.close();

                }


                catch (Exception e) {
                    Log.e(TAG, "Exception in Client Task  11111 ---> "+REMOTE_PORT.get(j) +"length = "+REMOTE_PORT.size());


                // UPDATING REMOTE_PORT LIST
                //lock.lock();
                    /*for(int y=0;y<REMOTE_PORT.size();y++)
                    {
                        Log.e(TAG, "Failed Port = "+REMOTE_PORT.get(j));

                            if (REMOTE_PORT.get(y).equals(REMOTE_PORT.get(j))) // REMOTE_PORT[j] IS FAILED NODE.
                            {
                                Log.e(TAG, "IN IF");
                            }
                            else
                            {
                                Log.e(TAG, "IN ELSE");
                                copy_first.add(REMOTE_PORT.get(y));
                            }

                    }


                    REMOTE_PORT.clear();
                    for(String s:copy_first)
                        REMOTE_PORT.add(s);
                        // REWRITING CONTENTS OF COPY BACK INTO REMOTE PORT

                for(int o=0;o<REMOTE_PORT.size();o++)
                    System.out.println("REMOTE PORT --> "+REMOTE_PORT.get(o));
                //lock.unlock();

*/
                    e.printStackTrace();
                }




            } // END OF FIRST FOR LOOP



            //multicast the message and final priority and send out R msg . IT WILL OBVIOUSLY HAVE RECEIVED ALL PROPOSED PRIORITIES AND IT HAS MAX PROPOSED PRIORITY*/
            sendingObj.type = "R";

            sendingObj.seen_by = 0;

            //System.out.println("******************************IN SECOND LOOP : "+REMOTE_PORT.size());

            for(int k = 0; k < REMOTE_PORT.size(); k++)
            {
                try {


                    //   lock.lock();
                    Socket socket_multicast = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT.get(k)));

                    // lock.unlock();

                    socket_multicast.setSoTimeout(1000);

                    PrintWriter out_multicast = new PrintWriter(socket_multicast.getOutputStream(), true);

                    String multicast_msg = sendingObj.type + ":" + sendingObj.p + ":" + sendingObj.PNum + ":" + sendingObj.msgId + ":" + sendingObj.sent_by + ":" + sendingObj.deliverable + ":" + sendingObj.msg;
                    out_multicast.println(multicast_msg + "\r\n");

                    //16 MARCH
                    socket_multicast.close();
                }
                catch(Exception e)
                {

                    //Log.e(TAG, "Exception in Client Task  222222 ---> " + REMOTE_PORT.get(k) + "length = " + REMOTE_PORT.size());

  /*                //  lock.lock();
                    // UPDATING REMOTE_PORT LIST

                    for(int y=0;y<REMOTE_PORT.size();y++)
                    {
                        Log.e(TAG, "Failed Port 2222 = "+REMOTE_PORT.get(k));

                        if (REMOTE_PORT.get(y).equals(REMOTE_PORT.get(k))) // REMOTE_PORT[j] IS FAILED NODE.
                        {
                            Log.e(TAG, "IN IF");
                        }
                        else
                        {
                            Log.e(TAG, "IN ELSE");
                            copy_second.add(REMOTE_PORT.get(y));
                        }

                    }


                    REMOTE_PORT.clear();

                    for(String s:copy_second)
                        REMOTE_PORT.add(s);
                    // REWRITING CONTENTS OF COPY BACK INTO REMOTE PORT

                    for(int o=0;o<REMOTE_PORT.size();o++)
                        System.out.println("REMOTE PORT  22222--> "+REMOTE_PORT.get(o));
*/
                    //                   lock.unlock();
                    e.printStackTrace();
                }

            } // END OF SECOND FOR LOOP

            return null;
        }

    }
}
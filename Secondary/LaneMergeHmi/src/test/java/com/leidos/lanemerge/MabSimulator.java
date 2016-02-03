package com.leidos.lanemerge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;

/**
 * Created by starkj on 11/29/15.
 *
 * Purpose:  to simulate both incoming and outgoing UDP traffic on the MAB side as it talks to the secondary processor (HMI).
 */
public class MabSimulator {

    //////////////////////////////////////////////////////////
    //Inner class to handle the incoming messages from the HMI

    public class InputThread implements Runnable {

        private DatagramSocket  socket_;
        private boolean         done_ = false;


        public void setup(DatagramSocket socket) {
            socket_ = socket;
            new Thread(this).start();
        }

        @Override
        public void run() {

            //loop until the shutdown signal comes
            while (!done_) {
                byte[] buf = new byte[32];
                DatagramPacket packet = new DatagramPacket(buf, 32);

                try {
                    socket_.receive(packet);
                    int bytesRead = packet.getLength();
                    if (bytesRead > 0) {
                        byte[] message = new byte[bytesRead];
                        message = Arrays.copyOf(packet.getData(), bytesRead);
                        displayMessage(message);
                    }
                } catch (IOException e) {
                    //move on; we don't expect messages to arrive very often
                }
            }
        }

        private void displayMessage(byte[] m) {
            System.out.print("Message received from HMI: ");
            for (int i = 0;  i < m.length;  ++i) {
                String out;
                out = String.format("%02x ", m[i]);
                System.out.print(out);
            }
            System.out.println(" ");
        }

        public void shutdown() {
            done_ = true;
        }
    }


    ////////////////////////////////////////////////////////
    //Inner class to handle the outgoing messages to the HMI

    public class OutputThread implements Runnable {

        private DatagramSocket      socket_;
        private InetSocketAddress   iAddr_;

        private boolean mergeRequest_ = false;
        private boolean mergeRequestAck_ = false;
        private boolean mergeMReady_ = false;
        private boolean mergeFReady_ = false;
        private boolean mergeMComplete_ = false;
        private boolean mergeFComplete_ = false;
        private MabSimulator parent_;

        private class StatusMsg {
            byte[] content = new byte[2];
            StatusMsg(int a, int b) { content[0] = (byte)a; content[1] = (byte)b; }
        }

        private StatusMsg[] sequence = {
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x01, 0x00),
                new StatusMsg(0x01, 0x00),  //5
                new StatusMsg(0x01, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),  //10
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x01, 0x02),
                new StatusMsg(0x00, 0x03), //should be the same as above when combined
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),  //20
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x04),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x08, 0x04),  //30
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),
                new StatusMsg(0x08, 0x00),  //40
                new StatusMsg(0x00, 0x00),
                new StatusMsg(0x0c, 0x04),
                new StatusMsg(0x0c, 0x04),
                new StatusMsg(0x0c, 0x00)
        };

        public void setup(DatagramSocket socket, InetSocketAddress addr, MabSimulator parent) {
            socket_ = socket;
            parent_ = parent;
            iAddr_ = addr;

            new Thread(this).start();
        }

        @Override
        public void run() {

            //set up for manual user control of the output_ data
            byte[] buf = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String input = "";

            //loop until exit command
            int cmd = 0;
            do {

                //display the menu of commands available
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    //don't worry about it
                }
                System.out.println(" ");
                System.out.println("Select an action:");
                System.out.println("0) Exit");
                System.out.println("1) Toggle merge request    " + status(mergeRequest_));
                System.out.println("2) Toggle Request received " + status(mergeRequestAck_));
                System.out.println("3) Toggle M Ready          " + status(mergeMReady_));
                System.out.println("4) Toggle F Ready          " + status(mergeFReady_));
                System.out.println("5) Toggle M Complete       " + status(mergeMComplete_));
                System.out.println("6) Toggle F Complete       " + status(mergeFComplete_));
                System.out.println("7) No change");
                System.out.println("8) No message");
                System.out.println("9) Rapid sequence");

                //wait for a command
                try {
                    input = in.readLine();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                cmd = Integer.valueOf(input);
                boolean skip = false;
                boolean rapid = false;

                //execute the user's command
                switch (cmd) {
                    case 0:
                        break;

                    case 1:
                        mergeRequest_ = !mergeRequest_;
                        break;

                    case 2:
                        mergeRequestAck_ = !mergeRequestAck_;
                        break;

                    case 3:
                        mergeMReady_ = !mergeMReady_;
                        break;

                    case 4:
                        mergeFReady_ = !mergeFReady_;
                        break;

                    case 5:
                        mergeMComplete_ = !mergeMComplete_;
                        break;

                    case 6:
                        mergeFComplete_ = !mergeFComplete_;
                        break;

                    //case 7 is default (send the same message as last time)

                    case 8: //send no data
                        skip = true;
                        break;

                    case 9: //rapid-fire a pile of messages
                        rapid = true;
                        break;

                    default:
                        break;
                }

                buf = buildMessage();

                //broadcast it on the specified UDP port
                try {
                    //if we're sending a single message then
                    if (buf != null  &&  !skip  &&  !rapid) {
                        int length = buf.length;
                        DatagramPacket packet = new DatagramPacket(buf, length, iAddr_);
                        socket_.send(packet);

                    //else if a rapid sequence then
                    }else if (rapid) {
                        fireRapidSequence();
                    }
                } catch (IOException e) {
                    System.out.println("IO Exception sending message: " + e.getMessage());
                    e.printStackTrace();
                    socket_.close();
                    return;
                } catch (Exception ee) {
                    System.out.println("Exception sending message: " + ee.getMessage());
                    ee.printStackTrace();
                    socket_.close();
                    return;
                }

            } while(cmd != 0);

            System.out.println("Shutting down.");
            socket_.close();
            parent_.shutdown();
        }

        private String status(boolean flag) {
            String result = "(currently ";
            if (flag) {
                result += "on)";
            }else {
                result += "off)";
            }
            return result;
        }

        private byte[] buildMessage() {
            byte[]  msg = { 0x00, 0x00 };
            byte    val = 0;

            if (mergeRequest_) {
                val |= 0x01;
            }

            if (mergeMReady_) {
                val |= 0x02;
            }

            if (mergeFReady_) {
                val |= 0x04;
            }

            if (mergeMComplete_) {
                val |= 0x08;
            }

            if (mergeFComplete_) {
                val |= 0x10;
            }

            if (mergeRequestAck_) {
                val |= 0x80;
            }

            //in the real MAB each byte will probably contain different values, but for our testing purposes
            // it is fine to just use the same value in each. It is important to send two bytes, however.
            msg[0] = val;
            msg[1] = val;
            return msg;
        }

        private void fireRapidSequence() {
            int i = 0;

            try{
                for (i = 0;  i < sequence.length;  ++i) {
                    DatagramPacket p = new DatagramPacket(sequence[i].content, 2, iAddr_);
                    socket_.send(p);
                    Thread.sleep(50);
                }
            } catch (Exception ee) {
                System.out.println("Exception in rapid-fire message: " + ee.getMessage());
                //ee.printStackTrace();
                socket_.close();
                return;
            }
        }
    }


    //////////////////////////////////////////////////////
    //main program

    private InputThread             input_ = null;
    private OutputThread            output_ = null;
    private DatagramSocket          socket_ = null;
    private InetSocketAddress       iAddr_ = null;

    public static void main(String[] args) {
        String host;
        if (args.length > 0){
            host = args[0];
        }else {
            host = "localhost";
        }

        int port = 5002;
        if (args.length > 1) {
            port = Integer.valueOf(args[1]);
        }

        new MabSimulator().doit(host, port);
    }

    public void doit(String host, int port) {
        socket_ = startup(host, port);

        input_ = new InputThread();
        input_.setup(socket_);

        output_ = new OutputThread();
        output_.setup(socket_, iAddr_, this);
    }

    private DatagramSocket startup(String host, int port) {
        //set up the network environment
        InetAddress addr = null;
        try {
            if (host == "localhost"){
                addr = InetAddress.getLocalHost();
            }else {
                addr = InetAddress.getByName(host);
            }
        } catch (UnknownHostException e) {
            System.out.println("Host exception in startup: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(10);
            iAddr_ = new InetSocketAddress(host, port);
        } catch (SocketException e1) {
            System.out.println("Datagram exception in network startup: " + e1.getMessage());
            e1.printStackTrace();
            return null;
        }
        System.out.println("Simulated MAB ready for messages on " + host + ":" + port);

        return socket;
    }

    private void shutdown() {
        input_.shutdown();
        socket_.close();
    }
}

package com.leidos.lanemerge.command;

import com.leidos.lanemerge.config.LaneMergeApplicationContext;
import com.leidos.lanemerge.logger.ILogger;
import com.leidos.lanemerge.logger.LoggerManager;
import com.leidos.lanemerge.services.LaneMergeService;
import com.leidos.lanemerge.ui.UiMessage;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * CommandStatusProcessor
 *
 * Listens for incoming status messages on mab.command.port
 * Forwards messages to MAB mab.host, mab.command.port
 *
 *
 */
public class CommandStatusProcessor implements Runnable {

    private static ILogger logger = LoggerManager.getLogger(CommandStatusProcessor.class);

    private AtomicBoolean bShutdown = new AtomicBoolean(false);
    private int maxPacketSize;
    private int udpTimeout;
    private int minUiDuration;

    // MAB configuration for Commands
    private DatagramSocket mabSocket;
    private String mabIp;
    private int mabUdpPort;
    private InetSocketAddress mabAddress;

    private DateTime lastTimeSent;
    private boolean latchMessages;
    private byte previousStatus;
    private LaneMergeService service;

    public CommandStatusProcessor(){
        this.maxPacketSize = LaneMergeApplicationContext.getInstance().getAppConfig().getIntValue("asd.maxpacketsize");
        this.udpTimeout = LaneMergeApplicationContext.getInstance().getAppConfig().getIntValue("udp.timeout");
        this.lastTimeSent = new DateTime();
        this.minUiDuration = LaneMergeApplicationContext.getInstance().getAppConfig().getIntValue("ui.duration");

        this.mabUdpPort = LaneMergeApplicationContext.getInstance().getAppConfig().getIntValue("mab.command.port");
        this.mabIp = LaneMergeApplicationContext.getInstance().getAppConfig().getProperty("mab.host");
        this.mabAddress = new InetSocketAddress(mabIp, mabUdpPort);

        this.latchMessages = LaneMergeApplicationContext.getInstance().getAppConfig().getProperty("ui.status.latch").equals("true");

        previousStatus = 0;
    }

    /**
     * initialize
     */
    public void initialize(){
        logger.info(ILogger.TAG_COMMAND, "Initializing MAB Commmand/Status Interface on port: " + mabUdpPort);

        //establish the UDP socket
        try   {
            mabSocket = new DatagramSocket(mabUdpPort);
            mabSocket.setSoTimeout(udpTimeout);
        }
        catch(Exception e)   {
            logger.error(ILogger.TAG_COMMAND, "Exception initializing mabSocket: " + e.getMessage());
        }

        //set up the communication service object
        service = LaneMergeApplicationContext.getInstance().getService();
        if (service != null) {
            new Thread(this).start();
        }else {
            logger.error(ILogger.TAG_COMMAND, "FATAL:  Unable to access the LaneMergeService object. CommandStatusProcessor not started.");
        }

    }


    @Override
    public void run() {
        logger.info(ILogger.TAG_COMMAND, "Starting MAB Command/Status thread for port: " + mabUdpPort);
        int latch = latchMessages ? 1 : 0;

        while (!bShutdown.get())  {
            byte[] buf = new byte[maxPacketSize];
            DatagramPacket p = new DatagramPacket(buf, maxPacketSize);

            try {
                // **** RECEIVE STATUS FROM MAB AND FORWARD TO UI

                //because we want to sleep a while below to give the user time to read what flashes on the screen, we may
                //accumulate several new packets on the UDP port since the previous read.  Therefore, we need to throw away
                //any identical duplicates (need to display anything that changes)

                //initialize counter to 0
                int bytesRead = 0;
                int numMsgs = 0;
                byte[] statusData = null;
                byte combinedStatus = 0;
                boolean different = false;

                do {
                    //read from the port
                    mabSocket.receive(p);
                    bytesRead = p.getLength();

                    //if the buffer is not empty then
                    if (bytesRead > 0) {
                        statusData = Arrays.copyOf(p.getData(), bytesRead);
                        //increment the counter
                        ++numMsgs;

                        //Each message should contain 2 bytes, one representing what is going on with our own vehicle,
                        // and the other representing messages received from the other vehicle via BSM (no guarantee on order).
                        // We are only concerned with the combination of these, so or them together.
                        if (bytesRead == 2) {
                            combinedStatus = (byte) (statusData[0] | statusData[1]);
                        }else if (bytesRead == 1) {
                            combinedStatus = (byte) statusData[0];
                            logger.warnf(ILogger.TAG_COMMAND, "Unexpected number of status bytes received from MAB: %d. Using this byte as the combined message.", bytesRead);
                        }else {
                            logger.warnf(ILogger.TAG_COMMAND, "Unexpected number of status bytes received from MAB: %d. Ignoring this message.", bytesRead);
                        }

                        //if it is different from the latest buffer pulled off then
                        if (combinedStatus != previousStatus) {
                            //store its contents as the latest
                            previousStatus = combinedStatus;
                            //indicate we have found one worth displaying and break out of loop
                            different = true;
                        }
                    }
                //while buffer is not empty and we have not found anything worthy of display
                }while (bytesRead > 0  &&  !different);
                logger.debugf(ILogger.TAG_COMMAND, "Pulled %d msgs off MAB port; different = %b", numMsgs, different);

                if (bytesRead > 0) {
                    logger.infof(ILogger.TAG_COMMAND, "MAB status handled %d bytes. Combined status = %02x", bytesRead, combinedStatus);
                    logger.debug(ILogger.TAG_COMMAND, "MAB raw status message: " + javax.xml.bind.DatatypeConverter.printHexBinary(statusData));

                    // send TO UI, but only if we have waited long enough for the previous message to be seen
                    Duration displayedSoFar = new Duration(lastTimeSent, new DateTime());
                    long diff = minUiDuration - displayedSoFar.getMillis();
                    if (diff > 0) {
                        try {
                            Thread.sleep(diff);
                        } catch (InterruptedException e) {
                        }
                    }
                    UiMessage uiMessage = new UiMessage(combinedStatus, latch);
                    service.sendUiMessage(uiMessage);
                    lastTimeSent = new DateTime();
                }
            } catch (SocketTimeoutException ste)   {
                // expected
            } catch (SocketException e) {
                logger.warn(ILogger.TAG_COMMAND, "read: socket exception ");
            } catch (IOException e) {
                logger.warn(ILogger.TAG_COMMAND, "read: IO exception ");
            }

        }

        closeConnection();
    }

    public void writeCommandToMab(byte[] command)   {

        try   {
            // Send command to MAB
            DatagramPacket sendPacket = new DatagramPacket(command, command.length, mabAddress);

            mabSocket.send(sendPacket);

            logger.debug(ILogger.TAG_COMMAND, "Sent " + command.length + " bytes to MAB Command: " + mabIp + ":" + mabUdpPort
                                                + "  Content=" + javax.xml.bind.DatatypeConverter.printHexBinary(command));
        } catch (SocketException e) {
            logger.warn(ILogger.TAG_COMMAND, "read: socket exception ");
        } catch (IOException e) {
            logger.warn(ILogger.TAG_COMMAND, "read: IO exception ");
        }
    }

    /**
     * terminate
     * Set the atomic boolean shutdown flag
     */
    public void terminate() {
        bShutdown.getAndSet(true);
    }


    /**
     * closeConnection
     */
    private void closeConnection(){
        logger.info(ILogger.TAG_COMMAND, "Stopping MAB Command thread for port: " + mabUdpPort);
        try {
            mabSocket.close();
        } catch (Exception e) {
            logger.error(ILogger.TAG_COMMAND, "Error closing MAB Command for ports: " + mabUdpPort + ", " + e.getMessage());
        }
    }

}
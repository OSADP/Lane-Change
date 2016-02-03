package com.leidos.lanemerge.ui;

import com.leidos.lanemerge.logger.ILogger;
import com.leidos.lanemerge.logger.LoggerManager;

/**
 * This class represents a UI message sent to the client via WebSockets.  It is converted to JSON.
 *
 */
public class UiMessage {

    private int status;         //the status byte holding bitflags for all of the status messages coming from the MAB
    private int latchMessages;  //1=message display should be latched, 0=do not latch

    public UiMessage(int stat, int latch)   {
        status = stat;
        latchMessages = latch;
    }

    public int getStatus() {
        return status;
    }

    public int getLatchMessages() {return latchMessages; }

    @Override
    public String toString()   {
        String msg = String.format("UIMessage [ status=%d, latchMessages=%d ]", status, latchMessages);
        return msg;
    }

}

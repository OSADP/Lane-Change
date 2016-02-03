package com.leidos.lanemerge.services;

import com.leidos.lanemerge.IConsumerInitializer;
import com.leidos.lanemerge.command.CommandStatusProcessor;
import com.leidos.lanemerge.config.AppConfig;
import com.leidos.lanemerge.consumers.dsrc.MabDsrcProcessor;
import com.leidos.lanemerge.consumers.dsrc.ObuDsrcProcessor;
import com.leidos.lanemerge.consumers.gps.GpsScheduledMessageConsumer;
import com.leidos.lanemerge.logger.ILogger;
import com.leidos.lanemerge.logger.LogEntry;
import com.leidos.lanemerge.logger.LoggerManager;
import com.leidos.lanemerge.command.CommandMgr;
import com.leidos.lanemerge.ui.UiMessage;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

@Service("LaneMergeService")
public class LaneMergeService implements DisposableBean {
    private static ILogger logger = LoggerManager.getLogger(LaneMergeService.class);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private SimpMessagingTemplate template = new SimpMessagingTemplate(new ExecutorSubscribableChannel());

    ObuDsrcProcessor obuDsrcProcessor;
    MabDsrcProcessor mabDsrcProcessor;

    GpsScheduledMessageConsumer gpsConsumer;

    CommandStatusProcessor commandProcessor;

    public void start()   {
        setLogLevel();
        LoggerManager.setRecordData(true);

        // allow running of components via configuration
        if ( appConfig.getIntValue("dsrc.enable") == 1 )   {
            initDsrcProcessor();
        }

        if ( appConfig.getIntValue("gps.enable") == 1 )   {
            initGpsConsumer();
        }

        if ( appConfig.getIntValue("command.enable") == 1 )   {
            initCommandProcessor();
        }
    }


    protected void initGpsConsumer()   {
        gpsConsumer = new GpsScheduledMessageConsumer();
        gpsConsumer.initialize();
        IConsumerInitializer gpsInitializer = gpsConsumer.getInitializer();

        Boolean bInit;
        try   {
            bInit = gpsInitializer.call();
            logger.debug(ILogger.TAG_GPS, "--- initGpsConsumer: ready to schedule messages.");
            if (bInit)   {
                gpsConsumer.sendScheduleMessages();
            }
            else   {
                // a consumer failed to initialize, stop the app
                logger.error("GPS", "GPS failed to initialize, please review the log");
            }
        }
        catch(Exception e)   {
            logger.error("GPS", "Error initializing GPS handshaking: " + e.getMessage());
            try { LoggerManager.writeToDisk(); } catch(Exception e2)   { logger.info(ILogger.TAG_GPS, "Error writing to log file:" + e2.getMessage()); }
        }
        logger.debug(ILogger.TAG_GPS, "--- bottom of initGpsConsumer.");
    }

    protected void initDsrcProcessor() {
    	
    	int udpTimeout = appConfig.getIntValue("udp.timeout");
    	int mabDsrcInport = appConfig.getIntValue("mab.dsrc.inport");
    	int mabDsrcOutport = appConfig.getIntValue("mab.dsrc.outport");

    	//due to port budget limitations on the MAB, we may need to consolidate the in & out ports there;
    	// no such problem talking to the OBU.
    	try {
			DatagramSocket mabInSocket = new DatagramSocket(mabDsrcInport);  //will be closed by MabDsrcProcessor or ObuDsrcProcessor
			mabInSocket.setSoTimeout(udpTimeout);
			DatagramSocket mabOutSocket = null;
			if (mabDsrcInport == mabDsrcOutport) {
				mabOutSocket = mabInSocket;
			}else {
				mabOutSocket = new DatagramSocket(mabDsrcOutport);  //will be closed by MabDsrcProcessor or ObuDsrcProcessor
				mabOutSocket.setSoTimeout(udpTimeout);
			}

	    	mabDsrcProcessor = new MabDsrcProcessor();
	        mabDsrcProcessor.initialize(mabInSocket);
	    	
	        obuDsrcProcessor = new ObuDsrcProcessor();
	        obuDsrcProcessor.initialize(mabOutSocket);
	        logger.infof(ILogger.TAG_EXECUTOR, "Established MAB sockets on ports %d (in), %d (out)", mabDsrcInport, mabDsrcOutport);

    	} catch (SocketException e) {
			logger.error(ILogger.TAG_EXECUTOR, "Unable to open MAB socket(s) for DSRC: " + e.toString());
		}
    }

    protected void initCommandProcessor()   {
        commandProcessor = new CommandStatusProcessor();
        commandProcessor.initialize();
        CommandMgr.getInstance().setCommandProcessor(commandProcessor);
    }


    //JOHN move this method to CommandStatusProcessor once it is proven to be working. It will eliminate
    // an ugly dependency in the package diagram from CommandStatusProcessor back to the Service.

    public synchronized void sendUiMessage(UiMessage uiMessage)   {
        logger.info(ILogger.TAG_DVI,  "Send to client: " + uiMessage.toString());
        try {
            template.convertAndSend("/topic/dvitopic", uiMessage);
        }catch (Exception e) {
            logger.warnf(ILogger.TAG_DVI, "Failed to send message to HMI client. Message=%s, Exception=%s", uiMessage.toString(), e.getMessage());
        }

        try   {
            LoggerManager.writeToDisk();
        }
        catch(IOException ioe)   {
            System.err.println("Error writing log to disk: " + ioe.getMessage());
        }

    }


    @Override
    public void destroy()   {

        if (obuDsrcProcessor != null)  obuDsrcProcessor.terminate();
        if (mabDsrcProcessor != null)  mabDsrcProcessor.terminate();
        if (gpsConsumer != null)  gpsConsumer.terminate();
        if (commandProcessor != null) commandProcessor.terminate();

        try   {
            Thread.sleep(100);
        }
        catch(Exception e) {};
        logger.info(ILogger.TAG_EXECUTOR,  "Destroying bean LaneMergeService via lifecycle destroy().");
    }

    /**
     * Set min log level
     *
     * If not configured or configured incorrectly, uses DEBUG
     */
    public void setLogLevel()   {
        String logLevel = appConfig.getProperty("log.level");

        LogEntry.Level enumLevel = null;

        try   {
            enumLevel = LogEntry.Level.valueOf(logLevel.toUpperCase());
        }
        catch(Exception e)   {
            logger.warn("EXEC", "log.level value improperly configured: " + logLevel);
            enumLevel = LogEntry.Level.DEBUG;
        }

        LoggerManager.setMinOutputToWrite(enumLevel);
    }

}

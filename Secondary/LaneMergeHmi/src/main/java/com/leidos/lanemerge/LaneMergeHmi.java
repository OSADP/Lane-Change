package com.leidos.lanemerge;

import com.leidos.lanemerge.config.LaneMergeApplicationContext;
import com.leidos.lanemerge.config.AppConfig;
import com.leidos.lanemerge.logger.ILogger;
import com.leidos.lanemerge.logger.LoggerManager;
import com.leidos.lanemerge.services.LaneMergeService;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

@ComponentScan
@EnableAutoConfiguration
public class LaneMergeHmi {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(LaneMergeHmi.class, args);

        // set our context for non spring managed classes
        LaneMergeApplicationContext.getInstance().setApplicationContext(context);
        AppConfig config = LaneMergeApplicationContext.getInstance().getAppConfig();

        //SimulatedDviExecutorService service = context.getBean(SimulatedDviExecutorService.class);
        LaneMergeService service = context.getBean(LaneMergeService.class);
        LaneMergeApplicationContext.getInstance().setService(service);

        DateTime now = new DateTime();
        DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYYMMddHHmmss");
        String logName = fmt.print(now);

        LoggerManager.setOutputFile(config.getProperty("log.path") + logName + ".log");
        LoggerManager.setRealTimeOutput(config.getLogRealTimeOutput());
        ILogger logger = LoggerManager.getLogger(LaneMergeHmi.class);

        logger.infof("TAG", "####### Lane merge server started ########");
        try   {
            LoggerManager.writeToDisk();
        }
        catch(IOException ioe)   {
            System.err.println("Error writing log to disk: " + ioe.getMessage());
        }

        service.start();

    }
}
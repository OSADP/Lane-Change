package com.leidos.lanemerge.rest;

import com.leidos.lanemerge.config.AppConfig;
import com.leidos.lanemerge.config.LaneMergeApplicationContext;
import com.leidos.lanemerge.logger.ILogger;
import com.leidos.lanemerge.logger.LoggerManager;
import com.leidos.lanemerge.rest.response.AjaxResponse;
import com.leidos.lanemerge.command.CommandMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class LaneMergeCmdController {

    @Autowired
    AppConfig appConfig;

    private static ILogger logger = LoggerManager.getLogger(LaneMergeCmdController.class);

    @RequestMapping("/setParameters")
    public AjaxResponse setParameters(@RequestParam(value="vehicleRole", required=true) int vehicleRole,
                                      @RequestParam(value="operatingSpeed", required=true) int speed)   {

        //ensure our commands are further apart than the timeout for reading them
        int delay = LaneMergeApplicationContext.getInstance().getAppConfig().getIntValue("udp.timeout") + 2;

        boolean result = true;

        String statusMessage =  "Setting vehicleRole = " + vehicleRole + ", operating speed = " + speed;
        logger.info("REST", statusMessage);

        CommandMgr cmd = CommandMgr.getInstance();

        try   {
            //send the vehicle role twice because tests show that frequently the first one does not arrive
            Thread.sleep(delay);
            cmd.setVehicleRole(vehicleRole);
            Thread.sleep(delay);
            cmd.setVehicleRole(vehicleRole);

            //send the operating speed command
            Thread.sleep(delay);
            cmd.setOperatingSpeed(speed);
        }
        catch(Exception e)   {
            result = false;
            statusMessage = "Error setting lane merge parameters: " + e.getMessage();
        }

        return new AjaxResponse(result, statusMessage);
    }

    @RequestMapping("/logUiEvent")
    public AjaxResponse logUiEvent(@RequestParam(value="eventDescrip", required=true) String eventDescrip) {
        String statusMessage = "Logging UI event: " + eventDescrip;

        logger.info("REST", " ");
        logger.info("REST", "########## UI Event: " + eventDescrip);
        logger.info("REST", " ");

        return new AjaxResponse(true, statusMessage);
    }

}

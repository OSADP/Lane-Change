package com.leidos.lanemerge.command;

import com.leidos.lanemerge.rest.response.AjaxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manages all driver commands coming from the HMI going to the MAB
 */

public class CommandMgr {
        private static Logger logger = LoggerFactory.getLogger(CommandMgr.class);

        private CommandStatusProcessor commandProcessor;

        ///// singleton management

        private CommandMgr() {
        }

        private static class CommandMgrHolder {
            private static final CommandMgr _instance = new CommandMgr();
        }

        public static CommandMgr getInstance()
        {
            return CommandMgrHolder._instance;
        }

        public void setCommandProcessor(CommandStatusProcessor cmd)   {
            commandProcessor = cmd;
        }

        ///// workhorse methods

        //It is impractical to use an enum for vehicle role, as we need ordinal values to communicate w/Javascript & MAB
        //Vechile role interface is specified as:
        // 1 - lead
        // 2 - merging
        // 3 - following

        public void setVehicleRole(int roleVal)   {
            if (commandProcessor != null)   {
                commandProcessor.writeCommandToMab(assembleVehicleRoleMessage(roleVal));
            }
        }

        public void setOperatingSpeed(int speed) {
            if (commandProcessor != null) {
                commandProcessor.writeCommandToMab(assembleOperatingSpeedMessage(speed));
            }
        }

        protected byte[] assembleVehicleRoleMessage(int role) {
            byte[] message = new byte[2];
            message[0] = (byte)0x81;        //indicates this is a vehicle role message
            message[1] = (byte)role;

            return message;
        }

        protected byte[] assembleOperatingSpeedMessage(int speed) {
            byte[] message = new byte[2];
            message[0] = (byte)0x82;        //indicates oper speed message
            message[1] = (byte)(speed < 127 ? speed : 127);

            return message;
        }
    }

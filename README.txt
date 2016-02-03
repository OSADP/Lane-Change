Open Source Overview
============================
Connected, Longitudinally Automated Lane Change software (aka Lane Change)
Version 1.2

Description:
This lane change software sets on top of the reusable platform software in the TFHRC's CARMA fleet of Cadillac SRXs. It provides algorithmic control for the connected vehicles performing a lane merge maneuver using DSRC v2v communications.  It also provides the driver-vehicle interface (DVI) that allows the driver of each vehicle to choose that vehicle's role in the experiment, to set the operating speed of the experiment (lead vehicle), and to display status of the experiment's progress by way of status flags in received BSMs.  The DVI software also handles data communication to/from the vehicle's Pinpoint position system and the OBU DSRC radio.

The software is built in two parts:
  * the control software is written in Simulink and runs on the vehicle's MicroAutobox II
  * the DVI software is written in Java and runs on the vehicle's secondary computer, which is an Ubuntu Linux PC.


Installation and removal instructions
-------------------------------------
Microautobox:  this is a complicated build & install process, which involves the software from the CARMA library as well.  It is described in the document "Lane Merge Software Installation Instructions.docx" in the MAB/docs directory.  Note that it is intended to work with version 1.1 of the CARMA software (also available from OSADP).

Secondary computer:  the DVI software runs on Java 1.7, which needs to be installed first.
All of its functionality and resources are packaged in a single jar file, which needs to be installed in a directory named /opt/lanemerge.  It will also need a directory named /opt/lanemerge/logs.


License information
-------------------
See the accompanying LICENSE file.


System Requirements
-------------------------
Microautobox: dSpace Microautobox II computer

Secondary processor:  
Minimum memory:  2 GB
Processing power:  Intel Core I3 @ 1.6 GHz or equivalent
Connectivity:  ethernet
Operating systems supported:  Ubuntu 14.04

Documentation
-------------
There is no software documentation available.

Web sites
---------
The software is distributed through the USDOT's JPO Open Source Application Development Portal (OSADP)
http://itsforge.net/ 

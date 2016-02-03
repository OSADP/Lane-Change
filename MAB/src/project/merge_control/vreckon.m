function VincentyReckoning(block)
%MSFUNTMPL_BASIC A Template for a Level-2 MATLAB S-Function
%   The MATLAB S-function is written as a MATLAB function with the
%   same name as the S-function. Replace 'msfuntmpl_basic' with the 
%   name of your S-function.
%
%   It should be noted that the MATLAB S-function is very similar
%   to Level-2 C-Mex S-functions. You should be able to get more
%   information for each of the block methods by referring to the
%   documentation for C-Mex S-functions.
%
%   Copyright 2003-2010 The MathWorks, Inc.

%%
%% The setup method is used to set up the basic attributes of the
%% S-function such as ports, parameters, etc. Do not add any other
%% calls to the main body of the function.
%%
setup(block);

%endfunction

%% Function: setup ===================================================
%% Abstract:
%%   Set up the basic characteristics of the S-function block such as:
%%   - Input ports
%%   - Output ports
%%   - Dialog parameters
%%   - Options
%%
%%   Required         : Yes
%%   C-Mex counterpart: mdlInitializeSizes
%%
function setup(block)

% Register number of ports
block.NumInputPorts  = 4;
block.NumOutputPorts = 3;

% Setup port properties to be inherited or dynamic
block.SetPreCompInpPortInfoToDynamic;
block.SetPreCompOutPortInfoToDynamic;

% Override input port properties
block.InputPort(1).Dimensions        = 1;
block.InputPort(1).DatatypeID  = 0;  % double
block.InputPort(1).Complexity  = 'Real';
block.InputPort(1).DirectFeedthrough = true;

block.InputPort(2).Dimensions        = 1;
block.InputPort(2).DatatypeID  = 0;  % double
block.InputPort(2).Complexity  = 'Real';
block.InputPort(2).DirectFeedthrough = true;

block.InputPort(3).Dimensions        = 1;
block.InputPort(3).DatatypeID  = 0;  % double
block.InputPort(3).Complexity  = 'Real';
block.InputPort(3).DirectFeedthrough = true;

block.InputPort(4).Dimensions        = 1;
block.InputPort(4).DatatypeID  = 0;  % double
block.InputPort(4).Complexity  = 'Real';
block.InputPort(4).DirectFeedthrough = true;

% Override output port properties
block.OutputPort(1).Dimensions       = 1;
block.OutputPort(1).DatatypeID  = 0; % double
block.OutputPort(1).Complexity  = 'Real';

block.OutputPort(2).Dimensions       = 1;
block.OutputPort(2).DatatypeID  = 0; % double
block.OutputPort(2).Complexity  = 'Real';

block.OutputPort(3).Dimensions       = 1;
block.OutputPort(3).DatatypeID  = 0; % double
block.OutputPort(3).Complexity  = 'Real';

block.InputPort(1).SamplingMode = 'Sample';
block.InputPort(2).SamplingMode = 'Sample';
block.InputPort(3).SamplingMode = 'Sample';
block.InputPort(4).SamplingMode = 'Sample';
block.OutputPort(1).SamplingMode = 'Sample';
block.OutputPort(2).SamplingMode = 'Sample';
block.OutputPort(3).SamplingMode = 'Sample';

% Register parameters
block.NumDialogPrms     = 0;

% Register sample times
%  [0 offset]            : Continuous sample time
%  [positive_num offset] : Discrete sample time
%
%  [-1, 0]               : Inherited sample time
%  [-2, 0]               : Variable sample time
block.SampleTimes = [0 0];

% Specify the block simStateCompliance. The allowed values are:
%    'UnknownSimState', < The default setting; warn and assume DefaultSimState
%    'DefaultSimState', < Same sim state as a built-in block
%    'HasNoSimState',   < No sim state
%    'CustomSimState',  < Has GetSimState and SetSimState methods
%    'DisallowSimState' < Error out when saving or restoring the model sim state
block.SimStateCompliance = 'DefaultSimState';

%% -----------------------------------------------------------------
%% The MATLAB S-function uses an internal registry for all
%% block methods. You should register all relevant methods
%% (optional and required) as illustrated below. You may choose
%% any suitable name for the methods and implement these methods
%% as local functions within the same file. See comments
%% provided for each function for more information.
%% -----------------------------------------------------------------

block.RegBlockMethod('PostPropagationSetup',    @DoPostPropSetup);
block.RegBlockMethod('InitializeConditions', @InitializeConditions);
block.RegBlockMethod('Start', @Start);
block.RegBlockMethod('Outputs', @Outputs);     % Required
block.RegBlockMethod('Update', @Update);
block.RegBlockMethod('Derivatives', @Derivatives);
block.RegBlockMethod('Terminate', @Terminate); % Required

%end setup

%%
%% PostPropagationSetup:
%%   Functionality    : Setup work areas and state variables. Can
%%                      also register run-time methods here
%%   Required         : No
%%   C-Mex counterpart: mdlSetWorkWidths
%%
function DoPostPropSetup(block)
block.NumDworks = 1;
  
  block.Dwork(1).Name            = 'x1';
  block.Dwork(1).Dimensions      = 1;
  block.Dwork(1).DatatypeID      = 0;      % double
  block.Dwork(1).Complexity      = 'Real'; % real
  block.Dwork(1).UsedAsDiscState = true;


%%
%% InitializeConditions:
%%   Functionality    : Called at the start of simulation and if it is 
%%                      present in an enabled subsystem configured to reset 
%%                      states, it will be called when the enabled subsystem
%%                      restarts execution to reset the states.
%%   Required         : No
%%   C-MEX counterpart: mdlInitializeConditions
%%
function InitializeConditions(block)

%end InitializeConditions


%%
%% Start:
%%   Functionality    : Called once at start of model execution. If you
%%                      have states that should be initialized once, this 
%%                      is the place to do it.
%%   Required         : No
%%   C-MEX counterpart: mdlStart
%%
function Start(block)

block.Dwork(1).Data = 0;

%end Start

%%
%% Outputs:
%%   Functionality    : Called to generate block outputs in
%%                      simulation step
%%   Required         : Yes
%%   C-MEX counterpart: mdlOutputs
%%
function Outputs(block)
% RECKON - Using the WGS-84 Earth ellipsoid, travel a given distance along
%          a given azimuth starting at a given initial point, and return
%          the endpoint within a few millimeters of accuracy, using
%          Vincenty's algorithm.
%
% USAGE:
% [lat2,lon2] = vreckon(lat1, lon1, s, a12)
%
% VARIABLES:
% lat1 = inital latitude (degrees)
% lon1 = initial longitude (degrees)
% s    = distance (meters)
% a12  = intial azimuth (degrees)
% lat2, lon2 = second point (degrees)
% a21  = reverse azimuth (degrees), at final point facing back toward the
%        intial point
%
% Original algorithm source:
% T. Vincenty, "Direct and Inverse Solutions of Geodesics on the Ellipsoid
% with Application of Nested Equations", Survey Review, vol. 23, no. 176,
% April 1975, pp 88-93.
% Available at: http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
%
% Notes: 
% (1) The Vincenty reckoning algorithm was transcribed verbatim into
%     JavaScript by Chris Veness. It was modified and translated to Matlab
%     by Michael Kleder. Mr. Veness's website is:
%     http://www.movable-type.co.uk/scripts/latlong-vincenty-direct.html
% (2) Error correcting code, polar error corrections, WGS84 ellipsoid
%     parameters, testing, and comments by Michael Kleder.
% (3) By convention, when starting at a pole, the longitude of the initial
%     point (otherwise meaningless) determines the longitude line along
%     which to traverse, and hence the longitude of the final point.
% (4) The convention noted in (3) above creates a discrepancy with VDIST
%     when the the intial or final point is at a pole. In the VDIST
%     function, when traversing from a pole, the azimuth is  0 when
%     heading away from the south pole and 180 when heading away from the
%     north pole. In contrast, this VRECKON function uses the azimuth as
%     noted in (3) above when traversing away form a pole.
% (5) In testing, where the traversal subtends no more than 178 degrees,
%     this function correctly inverts the VDIST function to within 0.2
%     millimeters of distance, 5e-10 degrees of forward azimuth,
%     and 5e-10 degrees of reverse azimuth. Precision reduces as test
%     points approach antipodal because the precision of VDIST is reduced
%     for nearly antipodal points. (A warning is given by VDIST.)
% (6) Tested but no warranty. Use at your own risk.
% (7) Ver 1.0, Michael Kleder, November 2007

lat1 = block.InputPort(1).Data;
lon1 = block.InputPort(2).Data;
a12 = block.InputPort(3).Data;
s = block.InputPort(4).Data;

if abs(lat1)>90
    error('Input latitude must be between -90 and 90 degrees, inclusive.')
end
a = 6378137; % semimajor axis
b = 6356752.31424518; % semiminor axis
f = 1/298.257223563; % flattening coefficient
lat1   = lat1 * .1745329251994329577e-1; % intial latitude in radians
lon1   = lon1 * .1745329251994329577e-1; % intial longitude in radians
% correct for errors at exact poles by adjusting 0.6 millimeters:
kidx = abs(pi/2-abs(lat1)) < 1e-10;
if any(kidx);
    lat1(kidx) = sign(lat1(kidx))*(pi/2-(1e-10));
end
alpha1 = a12 * .1745329251994329577e-1; % inital azimuth in radians
sinAlpha1 = sin(alpha1);
cosAlpha1 = cos(alpha1);
tanU1 = (1-f) * tan(lat1);
cosU1 = 1 / sqrt(1 + tanU1*tanU1);
sinU1 = tanU1*cosU1;
sigma1 = atan2(tanU1, cosAlpha1);
sinAlpha = cosU1 * sinAlpha1;
cosSqAlpha = 1 - sinAlpha*sinAlpha;
uSq = cosSqAlpha * (a*a - b*b) / (b*b);
A = 1 + uSq/16384*(4096+uSq*(-768+uSq*(320-175*uSq)));
B = uSq/1024 * (256+uSq*(-128+uSq*(74-47*uSq)));
sigma = s / (b*A);
sigmaP = 2*pi;
i = 0;
while (abs(sigma-sigmaP) > 1e-12)
    i = i+1;
    cos2SigmaM = cos(2*sigma1 + sigma);
    sinSigma = sin(sigma);
    cosSigma = cos(sigma);
    deltaSigma = B*sinSigma*(cos2SigmaM+B/4*(cosSigma*(-1+...
        2*cos2SigmaM*cos2SigmaM)-...
        B/6*cos2SigmaM*(-3+4*sinSigma*sinSigma)*(-3+...
        4*cos2SigmaM*cos2SigmaM)));
    sigmaP = sigma;
    sigma = s / (b*A) + deltaSigma;
end
tmp = sinU1*sinSigma - cosU1*cosSigma*cosAlpha1;
lat2 = atan2(sinU1*cosSigma + cosU1*sinSigma*cosAlpha1,...
    (1-f)*sqrt(sinAlpha*sinAlpha + tmp*tmp));
lambda = atan2(sinSigma*sinAlpha1, cosU1*cosSigma - ...
    sinU1*sinSigma*cosAlpha1);
C = f/16*cosSqAlpha*(4+f*(4-3*cosSqAlpha));
L = lambda - (1-C) * f * sinAlpha * (sigma + C*sinSigma*(cos2SigmaM+...
    C*cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)));
lon2 = lon1 + L;
% output degrees
lat2 = lat2 * 57.295779513082322865;
lon2 = lon2 * 57.295779513082322865; 
% lon2 = mod(lon2,360); % follow [0,360] convention
a21 = atan2(sinAlpha, -tmp); 
a21  = 180 + a21  * 57.295779513082322865; % note direction reversal
a21=mod(a21,360);

block.OutputPort(1).Data = lat2;
block.OutputPort(2).Data = lon2;
block.OutputPort(3).Data = a21;

%end Outputs

%%
%% Update:
%%   Functionality    : Called to update discrete states
%%                      during simulation step
%%   Required         : No
%%   C-MEX counterpart: mdlUpdate
%%
function Update(block)

block.Dwork(1).Data = block.InputPort(1).Data;

%end Update

%%
%% Derivatives:
%%   Functionality    : Called to update derivatives of
%%                      continuous states during simulation step
%%   Required         : No
%%   C-MEX counterpart: mdlDerivatives
%%
function Derivatives(block)

%end Derivatives

%%
%% Terminate:
%%   Functionality    : Called at the end of simulation for cleanup
%%   Required         : Yes
%%   C-MEX counterpart: mdlTerminate
%%
function Terminate(block)

%end Terminate

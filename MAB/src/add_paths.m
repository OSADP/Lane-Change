proj = simulinkproject;
addpath(strcat(proj.RootFolder, '/src/platform'),...
        strcat(proj.RootFolder, '/src/adapter'),...
        strcat(proj.RootFolder, '/src/adapter/models'),...
        strcat(proj.RootFolder, '/src/adapter/c_src'),...
        strcat(proj.RootFolder, '/src/data_files'),...
        strcat(proj.RootFolder, '/src/utilities'),...
        strcat(proj.RootFolder, '/src/test'),...
        strcat(proj.RootFolder, '/src/project'),...
        strcat(proj.RootFolder, '/src/project/merge_activation'),...
        strcat(proj.RootFolder, '/src/project/merge_control'),...
        strcat(proj.RootFolder, '/src/project/gap_control'),...
        strcat(proj.RootFolder, '/src/project/radar_switch'),...
        strcat(proj.RootFolder, '/src/project/vehicle_role'),...
        strcat(proj.RootFolder, '/src/project/HMI_UDP'),...
        strcat(proj.RootFolder, '/src/project/command_handler'),...
        strcat(proj.RootFolder, '/src/project/speed_controller'),...
        strcat(proj.RootFolder, '/src/project/dsrc_utils'),...
        strcat(proj.RootFolder, '/src/project/turn_signal'),...
        strcat(proj.RootFolder, '/src/project/simulink_utils'),...
        strcat(proj.RootFolder, '/src'));
        
Simulink.fileGenControl('set',...
                  'CacheFolder', strcat(proj.RootFolder, '/cache'), ...
                  'CodeGenFolder', strcat(proj.RootFolder, '/bld'), ...
                  'createDir', true);

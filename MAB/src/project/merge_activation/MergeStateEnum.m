classdef MergeStateEnum < Simulink.IntEnumType
  enumeration
    STARTUP(0)
    MERGE_REQUEST(1)
    M_MERGE_READY(2)
    F_MERGE_READY(4)
    M_MERGE_COMPLETE(8)
    F_MERGE_COMPLETE(16)
    F_MERGE_REQUEST_ACK(128)
  end
  methods (Static)
    function retVal = getDefaultValue()
      retVal = MergeStateEnum.STARTUP;
    end
  end
end 
classdef VehicleRoleEnum < Simulink.IntEnumType
  enumeration
    ERROR(0)
    LEAD(1)
    MERGE(2)
    FOLLOW(3)
  end
  methods (Static)
    function retVal = getDefaultValue()
      retVal = VehicleRoleEnum.ERROR;
    end
  end
end 
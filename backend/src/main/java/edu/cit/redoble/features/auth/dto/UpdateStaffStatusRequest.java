package edu.cit.redoble.features.auth.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateStaffStatusRequest {

    @NotNull
    private Boolean isStaff;

    public Boolean getIsStaff() {
        return isStaff;
    }

    public void setIsStaff(Boolean staff) {
        isStaff = staff;
    }
}

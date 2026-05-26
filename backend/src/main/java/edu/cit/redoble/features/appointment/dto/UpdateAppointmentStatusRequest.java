package edu.cit.redoble.features.appointment.dto;

import edu.cit.redoble.features.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

public class UpdateAppointmentStatusRequest {

    @NotNull
    private AppointmentStatus status;

    private Long doctorId;

    private String remarks;

    private String results;

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }
}
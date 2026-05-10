package edu.cit.redoble.features.appointment.dto;

public class CreateAppointmentRequest {
    private String date; // yyyy-MM-dd
    private String time; // HH:mm
    private String reason;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

package com.gymplatform.modules.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DayAgendaDto {
    private UUID staffId;
    private String staffName;
    private LocalDate date;
    private List<AppointmentDto> appointments;
    private int totalAppointments;
}

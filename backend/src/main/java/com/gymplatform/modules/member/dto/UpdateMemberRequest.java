package com.gymplatform.modules.member.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateMemberRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String healthNotes;
}

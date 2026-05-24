package com.collectx.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponseDTO {
    private Long   userId;
    private String name;
    private String email;
    private String role;
    private String status;
    private String phone;
}

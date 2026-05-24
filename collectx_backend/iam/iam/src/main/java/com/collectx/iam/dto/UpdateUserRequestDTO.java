package com.collectx.iam.dto;

import com.collectx.iam.entity.Role;
import com.collectx.iam.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequestDTO {

    private String name;

    @Email(message = "Email must be valid")
    private String email;

    private Role role;

    private UserStatus status;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
}

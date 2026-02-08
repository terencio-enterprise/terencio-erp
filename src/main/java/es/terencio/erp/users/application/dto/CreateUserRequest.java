package es.terencio.erp.users.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank 
    String username,
    
    @NotBlank 
    @Size(min = 6, max = 6, message = "POS PIN must be exactly 6 digits")
    @Pattern(regexp = "\\d+", message = "POS PIN must be numeric")
    String posPin,
    
    @NotBlank
    @Size(min = 8, message = "Backoffice password must be at least 8 characters")
    String backofficePassword,
    
    @NotBlank 
    String fullName,
    
    @NotBlank 
    String role,
    
    @NotNull 
    UUID storeId,
    
    List<String> permissions
) {}
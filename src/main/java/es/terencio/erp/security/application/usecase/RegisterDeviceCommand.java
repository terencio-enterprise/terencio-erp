package es.terencio.erp.security.application.usecase;

/**
 * Command for device registration.
 */
public record RegisterDeviceCommand(
        String registrationCode,
        String hardwareId,
        String appVersion) {
}

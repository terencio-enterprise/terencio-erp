package es.terencio.erp.devices.application.usecase;

/**
 * Command for device registration.
 */
public record RegisterDeviceCommand(
                String registrationCode,
                String hardwareId,
                String appVersion) {
}

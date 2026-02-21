package es.terencio.erp.marketing.application.dto.campaign;

import java.time.Instant;

import es.terencio.erp.marketing.domain.model.DeliveryStatus;

public record CampaignLogResponse(
        Long id,
        Long customerId,
        String customerName,
        String customerEmail,
        DeliveryStatus status,
        String errorMessage,
        Instant sentAt,
        Instant deliveredAt,
        Instant openedAt,
        Instant clickedAt,
        Instant bouncedAt,
        Instant unsubscribedAt,
        Instant complainedAt
) {}
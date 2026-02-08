package es.terencio.erp.sync.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * The aggregate object containing all updates for the POS.
 * The POS will iterate over these lists and perform UPSERTs (Insert or Update) locally.
 */
public record SyncResponseDto(
        Instant serverTime, // Current server time, POS saves this as new 'lastSync'
        List<SyncTaxDto> taxes,
        List<SyncTariffDto> tariffs,
        List<SyncProductDto> products,
        List<SyncBarcodeDto> barcodes,
        List<SyncPriceDto> prices,
        List<SyncPromotionDto> promotions,
        List<SyncCustomerDto> customers,
        List<SyncUserDto> users
) {
}

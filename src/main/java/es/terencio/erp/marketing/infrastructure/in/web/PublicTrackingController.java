package es.terencio.erp.marketing.infrastructure.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import es.terencio.erp.marketing.application.port.in.CampaignTrackingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/public/marketing/track")
@Tag(name = "Public Marketing Tracking", description = "Secure Pixel and Link tracking")
public class PublicTrackingController {

    private final CampaignTrackingUseCase trackingUseCase;

    public PublicTrackingController(CampaignTrackingUseCase trackingUseCase) {
        this.trackingUseCase = trackingUseCase;
    }

    @GetMapping(value = "/open/{logId}/pixel.gif", produces = MediaType.IMAGE_GIF_VALUE)
    @Operation(summary = "Email open tracking pixel")
    public ResponseEntity<byte[]> trackOpen(@PathVariable Long logId) {
        byte[] pixel = trackingUseCase.registerOpenAndGetPixel(logId);
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0L);
        return new ResponseEntity<>(pixel, headers, HttpStatus.OK);
    }

    @GetMapping("/click/{logId}")
    @Operation(summary = "Email secure link click tracking with expiration")
    public ResponseEntity<Void> trackClick(@PathVariable Long logId, @RequestParam("p") String payload, @RequestParam("sig") String signature) {
        String originalUrl = trackingUseCase.registerClickAndGetRedirectUrl(logId, payload, signature);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(originalUrl)).build();
    }
}

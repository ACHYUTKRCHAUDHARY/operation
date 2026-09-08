package com.achyut.operation.delivery;

import com.achyut.operation.api.ApiModels.LocationView;
import com.achyut.operation.service.TrackingService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
public class PublicTrackingController {
    private final DeliveryRepository deliveries;
    private final TrackingService trackingService;

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @GetMapping("/api/public/track/{token}")
    public PublicTrackingView track(@PathVariable String token) {
        Delivery d = deliveries.findByPublicTrackingToken(token)
            .orElseThrow(() -> new NoSuchElementException("Tracking link not found"));
        LocationView location = null;
        try { location = trackingService.latest(d.getId()); } catch (NoSuchElementException ignored) { }
        return new PublicTrackingView(d.getDeliveryNumber(), d.getAsset().getAssetCode(), d.getStatus(),
            d.getDestinationLatitude(), d.getDestinationLongitude(), d.getDestinationAddress(), d.getExpectedDeliveryAt(),
            d.getDispatchedAt(), d.getDeliveredAt(), location);
    }

    @GetMapping(value = "/api/public/track/{token}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String token) throws Exception {
        deliveries.findByPublicTrackingToken(token).orElseThrow(() -> new NoSuchElementException("Tracking link not found"));
        String url = publicBaseUrl.replaceAll("/$", "") + "/tracking.html?token=" + token;
        var matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 320, 320);
        var out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(out.toByteArray());
    }

    @GetMapping("/api/deliveries/{deliveryId}/tracking-access")
    public TrackingAccess trackingAccess(@PathVariable Long deliveryId) {
        Delivery d = deliveries.findById(deliveryId).orElseThrow(() -> new NoSuchElementException("Delivery not found"));
        String url = publicBaseUrl.replaceAll("/$", "") + "/tracking.html?token=" + d.getPublicTrackingToken();
        return new TrackingAccess(d.getPublicTrackingToken(), url, "/api/public/track/" + d.getPublicTrackingToken() + "/qr");
    }

    public record TrackingAccess(String token, String trackingUrl, String qrUrl) {}
    public record PublicTrackingView(String deliveryNumber, String assetCode, Delivery.DeliveryStatus status,
                                     Double destinationLatitude, Double destinationLongitude, String destinationAddress,
                                     Instant expectedDeliveryAt, Instant dispatchedAt, Instant deliveredAt,
                                     LocationView latestLocation) {}
}

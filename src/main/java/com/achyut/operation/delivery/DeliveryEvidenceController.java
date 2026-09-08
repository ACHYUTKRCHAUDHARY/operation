package com.achyut.operation.delivery;

import com.achyut.operation.api.ApiModels.DeliveryStatusRequest;
import com.achyut.operation.api.ApiModels.DeliveryView;
import com.achyut.operation.service.OperationsService;
import com.achyut.operation.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/deliveries/{deliveryId}")
@RequiredArgsConstructor
public class DeliveryEvidenceController {
    private final FileStorageService storage;
    private final OperationsService operationsService;

    @PostMapping(value = "/proof-of-delivery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DeliveryView uploadProof(@PathVariable Long deliveryId,
                                    @RequestPart("file") MultipartFile file,
                                    @RequestParam String receivedBy,
                                    @RequestParam(defaultValue = "system") String actor) {
        var stored = storage.store(file, "proof-of-delivery");
        return operationsService.updateDelivery(deliveryId,
            new DeliveryStatusRequest(Delivery.DeliveryStatus.DELIVERED, stored.url(), receivedBy, actor));
    }
}

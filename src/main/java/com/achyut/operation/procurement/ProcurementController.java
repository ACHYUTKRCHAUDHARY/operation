package com.achyut.operation.procurement;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {
    private final ProcurementService service;

    @PostMapping("/suppliers") @ResponseStatus(HttpStatus.CREATED)
    public Supplier createSupplier(@RequestBody ProcurementService.SupplierRequest request){return service.createSupplier(request);}
    @GetMapping("/suppliers") public List<Supplier> suppliers(){return service.suppliers();}

    @PostMapping("/purchase-requests") @ResponseStatus(HttpStatus.CREATED)
    public ProcurementService.PurchaseRequestView createRequest(@RequestBody ProcurementService.PurchaseRequestCreate request){return service.createRequest(request);}
    @GetMapping("/purchase-requests") public List<ProcurementService.PurchaseRequestView> requests(){return service.requests();}
    @PatchMapping("/purchase-requests/{id}/status")
    public ProcurementService.PurchaseRequestView status(@PathVariable Long id,@RequestBody ProcurementService.StatusChange request){return service.changeStatus(id,request);}
}

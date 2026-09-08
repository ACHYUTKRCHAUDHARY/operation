package com.achyut.operation.support;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {
    private final SupportService service;

    @PostMapping("/warranties") @ResponseStatus(HttpStatus.CREATED)
    public SupportService.WarrantyView createWarranty(@RequestBody SupportService.WarrantyRequest request){return service.createWarranty(request);}
    @GetMapping("/warranties") public List<SupportService.WarrantyView> warranties(){return service.warranties();}

    @PostMapping("/complaints") @ResponseStatus(HttpStatus.CREATED)
    public SupportService.ComplaintView createComplaint(@RequestBody SupportService.ComplaintRequest request){return service.createComplaint(request);}
    @GetMapping("/complaints") public List<SupportService.ComplaintView> complaints(@RequestParam(required=false) Long assetId){return service.complaints(assetId);}
    @PatchMapping("/complaints/{id}")
    public SupportService.ComplaintView updateComplaint(@PathVariable Long id,@RequestBody SupportService.ComplaintUpdate request){return service.updateComplaint(id,request);}
}

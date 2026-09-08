package com.achyut.operation.commercial;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/commercial")
@RequiredArgsConstructor
public class CommercialController {
    private final CommercialService service;

    @PostMapping("/work-orders/{workOrderId}/quotations") @ResponseStatus(HttpStatus.CREATED)
    public CommercialService.QuotationView createQuotation(@PathVariable Long workOrderId,@RequestBody CommercialService.QuotationRequest request){return service.createQuotation(workOrderId,request);}
    @PostMapping("/quotations/{quotationId}/approve")
    public CommercialService.QuotationView approve(@PathVariable Long quotationId,@RequestParam(required=false) String actor){return service.approveQuotation(quotationId,actor);}
    @GetMapping("/work-orders/{workOrderId}/quotations")
    public List<CommercialService.QuotationView> quotations(@PathVariable Long workOrderId){return service.quotations(workOrderId);}

    @PostMapping("/work-orders/{workOrderId}/invoices") @ResponseStatus(HttpStatus.CREATED)
    public CommercialService.InvoiceView createInvoice(@PathVariable Long workOrderId,@RequestBody CommercialService.InvoiceRequest request){return service.createInvoice(workOrderId,request);}
    @GetMapping("/work-orders/{workOrderId}/invoices")
    public List<CommercialService.InvoiceView> invoices(@PathVariable Long workOrderId){return service.invoices(workOrderId);}
    @PostMapping("/invoices/{invoiceId}/payments") @ResponseStatus(HttpStatus.CREATED)
    public CommercialService.PaymentView pay(@PathVariable Long invoiceId,@RequestBody CommercialService.PaymentRequest request){return service.recordPayment(invoiceId,request);}
    @GetMapping("/invoices/{invoiceId}/payments")
    public List<CommercialService.PaymentView> payments(@PathVariable Long invoiceId){return service.payments(invoiceId);}
}

package com.achyut.operation.commercial;

import com.achyut.operation.service.OperationsService;
import com.achyut.operation.work.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CommercialService {
    private final QuotationRepository quotations;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final WorkOrderRepository workOrders;
    private final OperationsService operations;

    public QuotationView createQuotation(Long workOrderId, QuotationRequest r) {
        WorkOrder w = workOrders.findById(workOrderId).orElseThrow(() -> new NoSuchElementException("Work order not found"));
        BigDecimal transport = nz(r.transportCharge()), tax = nz(r.taxAmount()), discount = nz(r.discount());
        BigDecimal total = r.baseAmount().add(transport).add(tax).subtract(discount);
        if (total.signum() < 0) throw new IllegalArgumentException("Quotation total cannot be negative");
        Quotation q = quotations.save(Quotation.builder().quotationNumber(number("QTN")).workOrder(w).baseAmount(r.baseAmount())
            .transportCharge(transport).taxAmount(tax).discount(discount).totalAmount(total).status(Quotation.Status.SENT).validUntil(r.validUntil()).build());
        w.setStatus(WorkOrder.WorkStatus.CUSTOMER_APPROVAL_PENDING);
        w.setEstimatedCost(total);
        operations.audit("WORK_ORDER", workOrderId, "QUOTATION_SENT", actor(r.actor()), q.getQuotationNumber()+" total "+total);
        return view(q);
    }

    public QuotationView approveQuotation(Long quotationId, String actor) {
        Quotation q = quotations.findById(quotationId).orElseThrow(() -> new NoSuchElementException("Quotation not found"));
        if (q.getStatus() == Quotation.Status.REJECTED || q.getStatus() == Quotation.Status.EXPIRED) throw new IllegalStateException("Quotation cannot be approved");
        q.setStatus(Quotation.Status.APPROVED); q.setApprovedAt(Instant.now());
        WorkOrder w = q.getWorkOrder(); w.setApprovedCost(q.getTotalAmount()); w.setStatus(WorkOrder.WorkStatus.APPROVED);
        operations.audit("WORK_ORDER", w.getId(), "QUOTATION_APPROVED", actor(actor), q.getQuotationNumber());
        return view(q);
    }

    public InvoiceView createInvoice(Long workOrderId, InvoiceRequest r) {
        WorkOrder w = workOrders.findById(workOrderId).orElseThrow(() -> new NoSuchElementException("Work order not found"));
        BigDecimal amount = r.amountDue()!=null ? r.amountDue() : (w.getApprovedCost()!=null ? w.getApprovedCost() : w.getEstimatedCost());
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Invoice amount must be positive");
        Invoice i = invoices.save(Invoice.builder().invoiceNumber(number("INV")).workOrder(w).amountDue(amount).amountPaid(BigDecimal.ZERO)
            .status(Invoice.Status.ISSUED).issueDate(LocalDate.now()).dueDate(r.dueDate()).build());
        operations.audit("WORK_ORDER", workOrderId, "INVOICE_ISSUED", actor(r.actor()), i.getInvoiceNumber()+" amount "+amount);
        return view(i);
    }

    public PaymentView recordPayment(Long invoiceId, PaymentRequest r) {
        Invoice i = invoices.findById(invoiceId).orElseThrow(() -> new NoSuchElementException("Invoice not found"));
        if (r.amount().signum() <= 0) throw new IllegalArgumentException("Payment amount must be positive");
        BigDecimal remaining = i.getAmountDue().subtract(i.getAmountPaid());
        if (r.amount().compareTo(remaining) > 0) throw new IllegalArgumentException("Payment exceeds outstanding amount");
        Payment p = payments.save(Payment.builder().invoice(i).amount(r.amount()).method(r.method()).referenceNumber(r.referenceNumber()).note(r.note()).build());
        i.setAmountPaid(i.getAmountPaid().add(r.amount()));
        i.setStatus(i.getAmountPaid().compareTo(i.getAmountDue()) >= 0 ? Invoice.Status.PAID : Invoice.Status.PARTIALLY_PAID);
        operations.audit("WORK_ORDER", i.getWorkOrder().getId(), "PAYMENT_RECORDED", actor(r.actor()), i.getInvoiceNumber()+" payment "+r.amount());
        return new PaymentView(p.getId(), invoiceId, p.getAmount(), p.getMethod(), p.getReferenceNumber(), p.getPaidAt());
    }

    public List<QuotationView> quotations(Long workOrderId){ return quotations.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId).stream().map(this::view).toList(); }
    public List<InvoiceView> invoices(Long workOrderId){ return invoices.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId).stream().map(this::view).toList(); }
    public List<PaymentView> payments(Long invoiceId){ return payments.findByInvoiceIdOrderByPaidAtDesc(invoiceId).stream().map(p->new PaymentView(p.getId(),invoiceId,p.getAmount(),p.getMethod(),p.getReferenceNumber(),p.getPaidAt())).toList(); }

    private QuotationView view(Quotation q){ return new QuotationView(q.getId(),q.getQuotationNumber(),q.getWorkOrder().getId(),q.getTotalAmount(),q.getStatus(),q.getValidUntil(),q.getApprovedAt()); }
    private InvoiceView view(Invoice i){ return new InvoiceView(i.getId(),i.getInvoiceNumber(),i.getWorkOrder().getId(),i.getAmountDue(),i.getAmountPaid(),i.getStatus(),i.getIssueDate(),i.getDueDate()); }
    private static BigDecimal nz(BigDecimal v){ return v==null?BigDecimal.ZERO:v; }
    private static String actor(String a){ return a==null||a.isBlank()?"system":a; }
    private static String number(String p){ return p+"-"+Instant.now().toEpochMilli()+"-"+UUID.randomUUID().toString().substring(0,4).toUpperCase(); }

    public record QuotationRequest(BigDecimal baseAmount, BigDecimal transportCharge, BigDecimal taxAmount, BigDecimal discount, LocalDate validUntil, String actor) {}
    public record InvoiceRequest(BigDecimal amountDue, LocalDate dueDate, String actor) {}
    public record PaymentRequest(BigDecimal amount, String method, String referenceNumber, String note, String actor) {}
    public record QuotationView(Long id,String quotationNumber,Long workOrderId,BigDecimal totalAmount,Quotation.Status status,LocalDate validUntil,Instant approvedAt) {}
    public record InvoiceView(Long id,String invoiceNumber,Long workOrderId,BigDecimal amountDue,BigDecimal amountPaid,Invoice.Status status,LocalDate issueDate,LocalDate dueDate) {}
    public record PaymentView(Long id,Long invoiceId,BigDecimal amount,String method,String referenceNumber,Instant paidAt) {}
}

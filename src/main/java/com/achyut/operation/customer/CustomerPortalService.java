package com.achyut.operation.customer;

import com.achyut.operation.asset.Asset;
import com.achyut.operation.asset.AssetRepository;
import com.achyut.operation.commercial.Invoice;
import com.achyut.operation.commercial.InvoiceRepository;
import com.achyut.operation.commercial.Payment;
import com.achyut.operation.commercial.PaymentRepository;
import com.achyut.operation.delivery.Delivery;
import com.achyut.operation.delivery.DeliveryRepository;
import com.achyut.operation.work.WorkOrder;
import com.achyut.operation.work.WorkOrderRepository;
import com.achyut.operation.work.WorkUpdateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerPortalService {
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkUpdateRepository workUpdateRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(String email) {
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));

        List<AssetView> assets = assetRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
            .map(a -> new AssetView(a.getId(), a.getAssetCode(), a.getType().name(), a.getStatus().name(), a.getSizeDescription(), a.getCurrentYardLocation(), a.getUpdatedAt()))
            .toList();

        List<WorkOrderView> workOrders = workOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
            .map(w -> new WorkOrderView(
                w.getId(), w.getOrderNumber(), w.getAsset().getAssetCode(), w.getWorkType().name(), w.getStatus().name(),
                w.getPriority().name(), w.getProgressPercent(), w.getExpectedCompletionAt(), w.getBlockedReason(), w.getEstimatedCost(), w.getApprovedCost(),
                workUpdateRepository.findByWorkOrderIdOrderByCreatedAtAsc(w.getId()).stream()
                    .map(u -> new TimelineView(u.getStage().name(), u.getStatus().name(), u.getNote(), u.getPhotoUrl(), u.getCreatedAt()))
                    .toList()
            )).toList();

        List<DeliveryView> deliveries = deliveryRepository.findByWorkOrderCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
            .map(d -> new DeliveryView(d.getId(), d.getDeliveryNumber(), d.getAsset().getAssetCode(), d.getStatus().name(), d.getVehicleNumber(),
                d.getDestinationAddress(), d.getExpectedDeliveryAt(), d.getDispatchedAt(), d.getDeliveredAt(), d.getPublicTrackingToken()))
            .toList();

        List<InvoiceView> invoices = invoiceRepository.findByWorkOrderCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
            .map(i -> new InvoiceView(i.getId(), i.getInvoiceNumber(), i.getWorkOrder().getOrderNumber(), i.getAmountDue(), i.getAmountPaid(), i.getStatus().name(), i.getIssueDate(), i.getDueDate()))
            .toList();

        List<PaymentView> payments = paymentRepository.findByInvoiceWorkOrderCustomerIdOrderByPaidAtDesc(customer.getId()).stream()
            .map(p -> new PaymentView(p.getId(), p.getInvoice().getInvoiceNumber(), p.getAmount(), p.getMethod(), p.getReferenceNumber(), p.getPaidAt()))
            .toList();

        long activeOrders = workOrders.stream().filter(w -> !List.of("COMPLETED", "CANCELLED").contains(w.status())).count();
        long activeDeliveries = deliveries.stream().filter(d -> !List.of("DELIVERED", "INSTALLED", "CANCELLED").contains(d.status())).count();
        BigDecimal outstanding = invoices.stream()
            .filter(i -> !"CANCELLED".equals(i.status()))
            .map(i -> i.amountDue().subtract(i.amountPaid()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardResponse(
            new CustomerView(customer.getId(), customer.getName(), customer.getCompanyName(), customer.getEmail(), customer.getPhone(), customer.getBillingAddress()),
            new SummaryView(assets.size(), activeOrders, activeDeliveries, outstanding),
            assets, workOrders, deliveries, invoices, payments
        );
    }

    public record DashboardResponse(CustomerView customer, SummaryView summary, List<AssetView> assets, List<WorkOrderView> workOrders,
                                    List<DeliveryView> deliveries, List<InvoiceView> invoices, List<PaymentView> payments) {}
    public record CustomerView(Long id, String name, String companyName, String email, String phone, String billingAddress) {}
    public record SummaryView(long assets, long activeOrders, long activeDeliveries, BigDecimal outstandingAmount) {}
    public record AssetView(Long id, String assetCode, String type, String status, String sizeDescription, String yardLocation, Instant updatedAt) {}
    public record WorkOrderView(Long id, String orderNumber, String assetCode, String workType, String status, String priority,
                                Integer progressPercent, Instant expectedCompletionAt, String blockedReason, BigDecimal estimatedCost,
                                BigDecimal approvedCost, List<TimelineView> timeline) {}
    public record TimelineView(String stage, String status, String note, String photoUrl, Instant createdAt) {}
    public record DeliveryView(Long id, String deliveryNumber, String assetCode, String status, String vehicleNumber, String destinationAddress,
                               Instant expectedDeliveryAt, Instant dispatchedAt, Instant deliveredAt, String trackingToken) {}
    public record InvoiceView(Long id, String invoiceNumber, String orderNumber, BigDecimal amountDue, BigDecimal amountPaid, String status,
                              LocalDate issueDate, LocalDate dueDate) {}
    public record PaymentView(Long id, String invoiceNumber, BigDecimal amount, String method, String referenceNumber, Instant paidAt) {}
}

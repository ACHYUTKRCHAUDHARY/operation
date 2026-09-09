package com.achyut.operation.commercial;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceIdOrderByPaidAtDesc(Long invoiceId);

    @EntityGraph(attributePaths = {"invoice", "invoice.workOrder"})
    List<Payment> findByInvoiceWorkOrderCustomerIdOrderByPaidAtDesc(Long customerId);
}

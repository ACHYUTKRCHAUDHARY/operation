package com.achyut.operation.commercial;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByWorkOrderIdOrderByCreatedAtDesc(Long workOrderId);
}

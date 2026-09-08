package com.achyut.operation.procurement;

import com.achyut.operation.inventory.*;
import com.achyut.operation.service.OperationsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcurementService {
    private final SupplierRepository suppliers;
    private final PurchaseRequestRepository requests;
    private final InventoryItemRepository inventory;
    private final OperationsService operations;

    public Supplier createSupplier(SupplierRequest r){
        return suppliers.save(Supplier.builder().name(r.name()).contactPerson(r.contactPerson()).phone(r.phone()).email(r.email()).address(r.address()).active(true).build());
    }

    public List<Supplier> suppliers(){ return suppliers.findAll(); }

    public PurchaseRequestView createRequest(PurchaseRequestCreate r){
        InventoryItem item=inventory.findById(r.inventoryItemId()).orElseThrow(()->new NoSuchElementException("Inventory item not found"));
        Supplier supplier=r.supplierId()==null?null:suppliers.findById(r.supplierId()).orElseThrow(()->new NoSuchElementException("Supplier not found"));
        if(r.quantity()==null||r.quantity().signum()<=0) throw new IllegalArgumentException("Quantity must be positive");
        PurchaseRequest pr=requests.save(PurchaseRequest.builder().requestNumber(number()).inventoryItem(item).supplier(supplier)
            .quantity(r.quantity()).expectedUnitCost(r.expectedUnitCost()).status(PurchaseRequest.Status.REQUESTED)
            .requestedBy(actor(r.requestedBy())).note(r.note()).build());
        operations.audit("INVENTORY",item.getId(),"PURCHASE_REQUESTED",actor(r.requestedBy()),pr.getRequestNumber()+" qty "+r.quantity());
        return view(pr);
    }

    public PurchaseRequestView changeStatus(Long id, StatusChange r){
        PurchaseRequest pr=requests.findById(id).orElseThrow(()->new NoSuchElementException("Purchase request not found"));
        PurchaseRequest.Status old=pr.getStatus();
        switch(r.status()){
            case APPROVED -> { pr.setApprovedBy(actor(r.actor())); pr.setApprovedAt(Instant.now()); }
            case RECEIVED -> {
                if(old!=PurchaseRequest.Status.ORDERED&&old!=PurchaseRequest.Status.APPROVED) throw new IllegalStateException("Purchase request must be approved or ordered before receipt");
                InventoryItem item=pr.getInventoryItem();
                item.setQuantityOnHand((item.getQuantityOnHand()==null?BigDecimal.ZERO:item.getQuantityOnHand()).add(pr.getQuantity()));
                if(pr.getExpectedUnitCost()!=null) item.setUnitCost(pr.getExpectedUnitCost());
                pr.setReceivedAt(Instant.now());
                operations.audit("INVENTORY",item.getId(),"STOCK_RECEIVED",actor(r.actor()),pr.getRequestNumber()+" +"+pr.getQuantity()+" "+item.getUnit());
            }
            default -> { }
        }
        pr.setStatus(r.status());
        return view(pr);
    }

    public List<PurchaseRequestView> requests(){ return requests.findAllByOrderByRequestedAtDesc().stream().map(this::view).toList(); }

    private PurchaseRequestView view(PurchaseRequest p){
        return new PurchaseRequestView(p.getId(),p.getRequestNumber(),p.getInventoryItem().getId(),p.getInventoryItem().getSku(),
            p.getSupplier()==null?null:p.getSupplier().getName(),p.getQuantity(),p.getExpectedUnitCost(),p.getStatus(),p.getRequestedBy(),p.getApprovedBy(),p.getRequestedAt(),p.getReceivedAt());
    }
    private static String actor(String a){return a==null||a.isBlank()?"system":a;}
    private static String number(){return "PR-"+Instant.now().toEpochMilli()+"-"+UUID.randomUUID().toString().substring(0,4).toUpperCase();}

    public record SupplierRequest(String name,String contactPerson,String phone,String email,String address){}
    public record PurchaseRequestCreate(Long inventoryItemId,Long supplierId,BigDecimal quantity,BigDecimal expectedUnitCost,String requestedBy,String note){}
    public record StatusChange(PurchaseRequest.Status status,String actor){}
    public record PurchaseRequestView(Long id,String requestNumber,Long inventoryItemId,String sku,String supplier,BigDecimal quantity,BigDecimal expectedUnitCost,PurchaseRequest.Status status,String requestedBy,String approvedBy,Instant requestedAt,Instant receivedAt){}
}

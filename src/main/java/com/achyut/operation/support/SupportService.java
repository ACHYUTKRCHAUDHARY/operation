package com.achyut.operation.support;

import com.achyut.operation.asset.*;
import com.achyut.operation.service.OperationsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportService {
    private final WarrantyRepository warranties;
    private final ComplaintRepository complaints;
    private final AssetRepository assets;
    private final OperationsService operations;

    public WarrantyView createWarranty(WarrantyRequest r){
        Asset asset=assets.findById(r.assetId()).orElseThrow(()->new NoSuchElementException("Asset not found"));
        if(r.endDate().isBefore(r.startDate())) throw new IllegalArgumentException("Warranty end date cannot be before start date");
        Warranty w=warranties.save(Warranty.builder().asset(asset).startDate(r.startDate()).endDate(r.endDate()).coverageDetails(r.coverageDetails()).status(Warranty.Status.ACTIVE).build());
        operations.audit("ASSET",asset.getId(),"WARRANTY_CREATED",actor(r.actor()),r.startDate()+" to "+r.endDate());
        return view(w);
    }

    public ComplaintView createComplaint(ComplaintRequest r){
        Asset asset=assets.findById(r.assetId()).orElseThrow(()->new NoSuchElementException("Asset not found"));
        LocalDate today=LocalDate.now();
        boolean covered=warranties.findTopByAssetIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(asset.getId(),today,today,Warranty.Status.ACTIVE).isPresent();
        Complaint c=complaints.save(Complaint.builder().complaintNumber(number()).asset(asset).description(r.description()).photoUrl(r.photoUrl())
            .warrantyCovered(covered).status(Complaint.Status.OPEN).build());
        operations.audit("ASSET",asset.getId(),"COMPLAINT_OPENED",actor(r.actor()),c.getComplaintNumber()+" warrantyCovered="+covered);
        return view(c);
    }

    public ComplaintView updateComplaint(Long id, ComplaintUpdate r){
        Complaint c=complaints.findById(id).orElseThrow(()->new NoSuchElementException("Complaint not found"));
        Complaint.Status old=c.getStatus();
        c.setStatus(r.status());
        if(r.assignedTo()!=null) c.setAssignedTo(r.assignedTo());
        if(r.resolutionNote()!=null) c.setResolutionNote(r.resolutionNote());
        if(r.status()==Complaint.Status.RESOLVED) c.setResolvedAt(Instant.now());
        operations.audit("ASSET",c.getAsset().getId(),"COMPLAINT_STATUS_CHANGED",actor(r.actor()),c.getComplaintNumber()+" "+old+" -> "+r.status());
        return view(c);
    }

    public List<ComplaintView> complaints(Long assetId){
        var list=assetId==null?complaints.findAllByOrderByCreatedAtDesc():complaints.findByAssetIdOrderByCreatedAtDesc(assetId);
        return list.stream().map(this::view).toList();
    }
    public List<WarrantyView> warranties(){ return warranties.findAll().stream().map(this::view).toList(); }

    private WarrantyView view(Warranty w){return new WarrantyView(w.getId(),w.getAsset().getId(),w.getAsset().getAssetCode(),w.getStartDate(),w.getEndDate(),w.getCoverageDetails(),w.getStatus());}
    private ComplaintView view(Complaint c){return new ComplaintView(c.getId(),c.getComplaintNumber(),c.getAsset().getId(),c.getAsset().getAssetCode(),c.getDescription(),c.getPhotoUrl(),c.isWarrantyCovered(),c.getStatus(),c.getAssignedTo(),c.getResolutionNote(),c.getCreatedAt(),c.getResolvedAt());}
    private static String actor(String a){return a==null||a.isBlank()?"system":a;}
    private static String number(){return "CMP-"+Instant.now().toEpochMilli()+"-"+UUID.randomUUID().toString().substring(0,4).toUpperCase();}

    public record WarrantyRequest(Long assetId,LocalDate startDate,LocalDate endDate,String coverageDetails,String actor){}
    public record ComplaintRequest(Long assetId,String description,String photoUrl,String actor){}
    public record ComplaintUpdate(Complaint.Status status,String assignedTo,String resolutionNote,String actor){}
    public record WarrantyView(Long id,Long assetId,String assetCode,LocalDate startDate,LocalDate endDate,String coverageDetails,Warranty.Status status){}
    public record ComplaintView(Long id,String complaintNumber,Long assetId,String assetCode,String description,String photoUrl,boolean warrantyCovered,Complaint.Status status,String assignedTo,String resolutionNote,Instant createdAt,Instant resolvedAt){}
}

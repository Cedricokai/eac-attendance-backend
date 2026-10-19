package com.example.eac_backend_project.entity.finance;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="petty_cash_requests", indexes={@Index(name="idx_petty_cash_status",columnList="status"),@Index(name="idx_petty_cash_requester",columnList="requester_id")}) @Data
public class PettyCashRequest {
    private Long inventoryRequestId;
    private String inventoryRequestNumber;
    private Long createdByUserId;
    private String createdByName;

 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=32) private String reference;
 @Column(name="voucher_number",unique=true,length=32) private String voucherNumber;
 @Column(name="requester_id",nullable=false) private Long requesterId;
 @Column(name="requester_name",nullable=false,length=150) private String requesterName;
 @Column(name="requester_email",nullable=false,length=180) private String requesterEmail;
 @Column(name="cycle_id") private Long cycleId;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
 @Column(name="approved_amount",precision=19,scale=2) private BigDecimal approvedAmount;
 @Column(nullable=false,length=100) private String category;
 @Column(nullable=false,columnDefinition="TEXT") private String purpose;
 @Column(name="cost_center",length=120) private String costCenter;
 @Column(nullable=false,length=32) private String status="PENDING_ACCOUNTS";
 @Column(name="accounts_notes",columnDefinition="TEXT") private String accountsNotes;
 @Column(name="processed_by",length=120) private String processedBy;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 @Column(name="processed_at") private LocalDateTime processedAt;
 @OneToMany(mappedBy="request",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.EAGER) @OrderBy("id ASC") private List<PettyCashItem> items=new ArrayList<>();
 @Column(name="actual_amount",precision=19,scale=2) private BigDecimal actualAmount;
 @Column(name="paid_to",length=150) private String paidTo;
 @Column(name="payment_method",length=50) private String paymentMethod;
 @Column(name="payment_reference",length=120) private String paymentReference;
 @Column(name="receipt_reference",length=500) private String receiptReference;
 @Column(name="receipt_file_name",length=255) private String receiptFileName;
 @Column(name="receipt_content_type",length=150) private String receiptContentType;
 @Column(name="receipt_file_size") private Long receiptFileSize;
 @Column(name="receipt_uploaded_at") private LocalDateTime receiptUploadedAt;
 @Column(name="voucher_notes",columnDefinition="TEXT") private String voucherNotes;
 @Column(name="finalized_by",length=120) private String finalizedBy;
 @Column(name="finalized_at") private LocalDateTime finalizedAt;
}

package com.example.eac_backend_project.transport;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.math.BigDecimal;
import com.example.eac_backend_project.entity.transport.*;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.files.FileItem;

@MappedSuperclass @Getter @Setter
abstract class TransportRow {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Version long revision;
 @Column(nullable=false) Instant createdAt=Instant.now();
 @Column(nullable=false) Instant updatedAt=Instant.now();
 @ManyToOne(fetch=FetchType.LAZY) User createdBy;
 Instant archivedAt;
 @PreUpdate void changed(){updatedAt=Instant.now();}
}

@Entity @Table(name="transport_vehicle_profiles",indexes={@Index(name="idx_transport_vehicle_profiles_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportVehicleProfile extends TransportRow {
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false,unique=true) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="driver_id") Drivers driver;
 @Column(length=100) String make;
 @Column(length=100) String model;
 @Column(length=100) String category;
 @Column(length=120) String engineNumber;
 @Column(length=60) String transmission;
 @Column(length=60) String ownershipType;
 @Column(length=120) String department;
 @Column(length=180) String project;
 LocalDate acquisitionDate;
 @Column(precision=19,scale=3) BigDecimal initialOdometer;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="primary_image_id",foreignKey=@ForeignKey(name="fk_transport_primary_image",foreignKeyDefinition="FOREIGN KEY (primary_image_id) REFERENCES file_items(id) ON DELETE SET NULL")) FileItem primaryImage;
 @Column(length=40) String operationalStatus="AVAILABLE";
 Instant downtimeStartedAt;
}

@Entity @Table(name="transport_driver_profiles",indexes={@Index(name="idx_transport_driver_profiles_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportDriverProfile extends TransportRow {
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="driver_id",nullable=false,unique=true) Drivers driver;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="account_id",unique=true) User account;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="employee_id") Employee employee;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="photo_file_id",foreignKey=@ForeignKey(name="fk_transport_driver_photo",foreignKeyDefinition="FOREIGN KEY (photo_file_id) REFERENCES file_items(id) ON DELETE SET NULL")) FileItem photo;
 @Column(length=254) String email;
 @Column(length=60) String licenseClass;
 @Column(length=120) String department;
 @Column(length=180) String project;
 Instant lastInvitationAt;
 boolean managedAccount=false;
 boolean notificationsEnabled=true;
 boolean active=true;
}

@Entity @Table(name="transport_odometer",indexes={@Index(name="idx_transport_odometer_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportOdometer extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @Column(precision=19,scale=3) BigDecimal reading;
 LocalDate readingDate;
 @Column(length=60) String source;
 @Column(length=120) String reference;
 Instant supersededAt;
 @Lob @Column(columnDefinition="TEXT") String supersededReason;
 boolean correction;
 @Column(length=1000) String reason;
}

@Entity @Table(name="transport_documents",indexes={@Index(name="idx_transport_documents_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportDocument extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @Column(length=80) String documentType;
 @Column(length=180) String referenceNumber;
 LocalDate issueDate;
 LocalDate expiryDate;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="file_id",foreignKey=@ForeignKey(name="fk_transport_document_file",foreignKeyDefinition="FOREIGN KEY (file_id) REFERENCES file_items(id) ON DELETE SET NULL")) FileItem file;
 @Column(length=180) String originalFileName;
 Long originalFileId;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="previous_document_id") TransportDocument previousDocument;
 boolean active=true;
 @Lob @Column(columnDefinition="TEXT") String notes;
}

@Entity @Table(name="transport_components",indexes={@Index(name="idx_transport_components_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportComponent extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @Column(length=120) String name;
 @Column(length=100) String category;
 LocalDate lastServiceDate;
 @Column(precision=19,scale=3) BigDecimal lastServiceMileage;
 @Column(precision=19,scale=3) BigDecimal intervalKm;
 Integer intervalDays;
 Integer intervalMonths;
 @Column(precision=19,scale=3) BigDecimal nextServiceMileage;
 LocalDate nextServiceDate;
 LocalDate replacementDate;
 @Column(precision=19,scale=2) BigDecimal replacementCost;
 @Column(length=180) String supplier;
 @Column(length=180) String technician;
 @Lob @Column(columnDefinition="TEXT") String notes;
 @Lob @Column(columnDefinition="TEXT") String checklist;
 @Column(precision=19,scale=3) BigDecimal warningKm;
 Integer warningDays;
}

@Entity @Table(name="transport_faults",indexes={@Index(name="idx_transport_faults_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportFault extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="reporter_id",nullable=false) User reporter;
 @Column(length=90) String reference;
 @Column(length=120) String category;
 @Lob @Column(columnDefinition="TEXT") String description;
 @Column(length=20) String severity="MEDIUM";
 @Column(length=40) String status="SUBMITTED";
 @Column(length=255) String location;
 @Column(precision=19,scale=3) BigDecimal odometer;
 boolean operational=true;
 @Lob @Column(columnDefinition="TEXT") String reviewNotes;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by_id") User reviewedBy;
 Instant reviewedAt;
 @Column(unique=true,length=100) String requestKey;
 @Column(length=64) String requestHash;
}

@Entity @Table(name="transport_work_orders",indexes={@Index(name="idx_transport_work_orders_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportWorkOrder extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fault_id") TransportFault fault;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="component_id") TransportComponent component;
 @Column(length=90) String reference;
 @Column(length=30) String maintenanceType="PREVENTIVE";
 @Column(length=180) String title;
 @Column(length=180) String componentNameAtCompletion;
 boolean replacement;
 @Column(length=40) String status="SCHEDULED";
 LocalDate scheduledDate;
 LocalDate completedDate;
 @Column(precision=19,scale=3) BigDecimal completionMileage;
 @Column(precision=19,scale=2) BigDecimal partsCost;
 @Column(precision=19,scale=2) BigDecimal labourCost;
 @Column(length=180) String supplier;
 @Column(length=180) String technician;
 @Lob @Column(columnDefinition="TEXT") String checklist;
 @Lob @Column(columnDefinition="TEXT") String completionNotes;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="verified_by_id") User verifiedBy;
 Instant verifiedAt;
 Instant startedAt;
 Instant completedAt;
}

@Entity @Table(name="transport_inspection_templates",indexes={@Index(name="idx_transport_inspection_templates_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportInspectionTemplate extends TransportRow {
 @Column(length=180) String name;
 @Column(length=30) String frequency="DAILY";
 Integer intervalDays;
 @Lob @Column(columnDefinition="TEXT") String description;
 boolean active=true;
}

@Entity @Table(name="transport_inspection_items",indexes={@Index(name="idx_transport_inspection_items_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportInspectionItem extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="template_id",nullable=false) TransportInspectionTemplate template;
 @Column(length=255) String label;
 int position;
 boolean safetyCritical;
}

@Entity @Table(name="transport_inspection_assignments",indexes={@Index(name="idx_transport_inspection_assignments_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportInspectionAssignment extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="template_id",nullable=false) TransportInspectionTemplate template;
 LocalDate nextDue;
 boolean active=true;
}

@Entity @Table(name="transport_inspections",indexes={@Index(name="idx_transport_inspections_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportInspection extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignment_id") TransportInspectionAssignment assignment;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="template_id",nullable=false) TransportInspectionTemplate template;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="inspector_id",nullable=false) User inspector;
 LocalDate inspectionDate;
 @Column(length=30) String outcome;
 @Lob @Column(columnDefinition="TEXT") String findings;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fault_id") TransportFault fault;
 @Column(unique=true,length=100) String requestKey;
 @Column(length=64) String requestHash;
}

@Entity @Table(name="transport_inspection_results",indexes={@Index(name="idx_transport_inspection_results_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportInspectionResult extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="inspection_id",nullable=false) TransportInspection inspection;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id",nullable=false) TransportInspectionItem item;
 @Column(length=255) String label;
 @Column(length=30) String outcome;
 @Lob @Column(columnDefinition="TEXT") String notes;
}

@Entity @Table(name="transport_procurement",indexes={@Index(name="idx_transport_procurement_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportProcurement extends TransportRow {
 @Column(length=100) String category;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fault_id") TransportFault fault;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="work_order_id") TransportWorkOrder workOrder;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="requested_by_id",nullable=false) User requestedBy;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assigned_to_id") User assignedTo;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="approved_by_id") User approvedBy;
 @Column(length=90) String reference;
 @Column(length=40) String status="DRAFT";
 @Column(length=20) String priority="MEDIUM";
 LocalDate requestDate;
 LocalDate requiredDate;
 @Lob @Column(columnDefinition="TEXT") String justification;
 @Lob @Column(columnDefinition="TEXT") String reviewNotes;
 Instant approvedAt;
 Instant receivedAt;
 @Column(length=180) String purchaseReference;
 @Column(length=180) String supplier;
 @Column(unique=true,length=100) String requestKey;
 @Column(length=64) String requestHash;
}

@Entity @Table(name="transport_procurement_items",indexes={@Index(name="idx_transport_procurement_items_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportProcurementItem extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="request_id",nullable=false) TransportProcurement request;
 @Column(length=180) String name;
 @Column(length=40) String unit;
 @Column(precision=19,scale=3) BigDecimal quantity;
 @Column(precision=19,scale=2) BigDecimal estimatedUnitCost;
 @Column(precision=19,scale=3) BigDecimal receivedQuantity;
 @Column(precision=19,scale=2) BigDecimal actualUnitCost;
}

@Entity @Table(name="transport_assets",indexes={@Index(name="idx_transport_assets_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportAsset extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="file_id",foreignKey=@ForeignKey(name="fk_transport_asset_file",foreignKeyDefinition="FOREIGN KEY (file_id) REFERENCES file_items(id) ON DELETE SET NULL")) FileItem file;
 Long originalFileId;
 @Column(unique=true,length=120) String requestKey;
 @Column(length=64) String requestHash;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fuel_id") Fuel fuel;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fault_id") TransportFault fault;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="work_order_id") TransportWorkOrder workOrder;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="inspection_id") TransportInspection inspection;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="procurement_id") TransportProcurement procurement;
 @Column(length=30) String purpose;
 @Column(length=180) String name;
 @Column(length=120) String mediaType;
 long sizeBytes;
 boolean removed;
}

@Entity @Table(name="transport_fuel_details",indexes={@Index(name="idx_transport_fuel_details_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportFuelDetail extends TransportRow {
 @Column(length=120,unique=true) String requestKey;
 @Column(length=64) String requestHash;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="fuel_id",nullable=false,unique=true) Fuel fuel;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="driver_id") Drivers driver;
 @Column(precision=19,scale=2) BigDecimal pricePerLitre;
 @Column(length=40) String fuelType;
 @Column(length=180) String station;
 @Column(length=60) String paymentMethod;
 @Column(length=180) String transactionReference;
 @Lob @Column(columnDefinition="TEXT") String remarks;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="receipt_id") TransportAsset receipt;
 @Column(precision=19,scale=3) BigDecimal odometer;
}

@Entity @Table(name="transport_maintenance_details",indexes={@Index(name="idx_transport_maintenance_details_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportMaintenanceDetail extends TransportRow {
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="maintenance_id",nullable=false,unique=true) Maintenance maintenance;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="vehicle_id",nullable=false) Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="work_order_id") TransportWorkOrder workOrder;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="component_id") TransportComponent component;
 @Column(precision=19,scale=3) BigDecimal serviceMileage;
 @Lob @Column(columnDefinition="TEXT") String notes;
}

@Entity @Table(name="transport_timeline",indexes={@Index(name="idx_transport_timeline_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportTimeline extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="vehicle_id") Vehicles vehicle;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_id") User actor;
 @Column(length=70) String eventType;
 @Lob @Column(columnDefinition="TEXT") String description;
 @Column(length=120) String reference;
 @Lob @Column(columnDefinition="TEXT") String details;
}

@Entity @Table(name="transport_notifications",indexes={@Index(name="idx_transport_notifications_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportNotification extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="recipient_id") User recipient;
 @Column(length=254) String email;
 @Column(length=70) String eventType;
 @Column(length=20) String priority="NORMAL";
 @Column(length=255) String title;
 @Lob @Column(columnDefinition="TEXT") String body;
 @Column(length=500) String targetPath;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="vehicle_id") Vehicles vehicle;
 @Column(length=120) String reference;
 Instant readAt;
 boolean inApp=true;
 boolean emailEnabled;
 @Column(length=30) String deliveryStatus="PENDING";
 int attempts;
 Instant nextAttemptAt=Instant.now();
 Instant leaseUntil;
 Instant deliveredAt;
 @Column(length=500) String lastError;
 @Column(unique=true,length=180) String dedupeKey;
}

@Entity @Table(name="transport_policy",indexes={@Index(name="idx_transport_policy_active",columnList="archived_at,updated_at")}) @Getter @Setter
class TransportPolicy extends TransportRow {
 @Column(unique=true,length=30) String scope="GLOBAL";
 @Column(length=180) String companyName="EAC Electrical Solutions Limited";
 @Column(length=10) String currency="GHS";
 @Column(length=60) String timezone="Africa/Accra";
 @Column(length=10) String distanceUnit="km";
 @Lob @Column(columnDefinition="TEXT") String adminEmails;
 @Lob @Column(columnDefinition="TEXT") String faultEmails;
 @Lob @Column(columnDefinition="TEXT") String procurementEmails;
 @Lob @Column(columnDefinition="TEXT") String supervisorEmails;
 @Lob @Column(columnDefinition="TEXT") String escalationEmails;
 @Column(length=100) String dvlaThresholds="30,14,7,0";
 @Column(length=100) String insuranceThresholds="30,14,7,0";
 @Column(length=100) String roadworthyThresholds="30,14,7,0";
 @Column(length=100) String licenseThresholds="30,14,7,0";
 @Column(length=100) String maintenanceDays="30,7,0";
 @Column(length=100) String maintenanceKm="1000,500,0";
 @Lob @Column(columnDefinition="TEXT") String vehicleCategories="Car,Van,Pickup,Truck,Bus,Heavy equipment";
 @Lob @Column(columnDefinition="TEXT") String documentTypes="DVLA,INSURANCE,ROADWORTHY,INSPECTION,OTHER";
 @Lob @Column(columnDefinition="TEXT") String componentCategories="Engine,Filters,Brakes,Tyres,Battery,Transmission,Cooling,Electrical,Other";
 @Lob @Column(columnDefinition="TEXT") String procurementCategories="Parts,Consumables,Labour,Other";
 int reminderDays=1;
 int forecastMinReadings=2;
 int forecastMaxAgeDays=60;
 boolean emailEnabled=false;
 boolean inAppEnabled=true;
 boolean notifyDriver=true;
 boolean notifyAdmin=true;
 boolean criticalEscalation=true;
 boolean allowMultipleVehiclesPerDriver=true;
 boolean driverFuelEnabled=true;
 boolean driverMileageEnabled=true;
}

@Entity @Table(name="transport_part_receipts",indexes={@Index(name="idx_transport_receipt_batch",columnList="batch_key"),@Index(name="idx_transport_receipt_date",columnList="received_date")}) @Getter @Setter
class TransportPartReceipt extends TransportRow {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="request_id",nullable=false) TransportProcurement request;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id",nullable=false) TransportProcurementItem item;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="received_by_id",nullable=false) User receivedBy;
 @Column(precision=19,scale=3) BigDecimal quantity;
 @Column(precision=19,scale=2) BigDecimal unitCost;
 LocalDate receivedDate;
 @Column(length=180) String reference;
 @Column(unique=true,length=100) String requestKey;
 @Column(length=64) String requestHash;
 @Column(length=100) String batchKey;
}

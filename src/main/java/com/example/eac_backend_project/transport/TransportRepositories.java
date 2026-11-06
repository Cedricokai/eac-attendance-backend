package com.example.eac_backend_project.transport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.Instant;

@NoRepositoryBean
interface TransportRepository<T extends TransportRow> extends JpaRepository<T,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from #{#entityName} r where r.id=:id") Optional<T> lock(@Param("id")Long id);
 List<T> findByArchivedAtIsNullOrderByCreatedAtDesc();
}
interface TransportVehicleProfiles extends TransportRepository<TransportVehicleProfile> {Optional<TransportVehicleProfile> findByVehicle_Id(Long id);List<TransportVehicleProfile> findByDriver_Id(Long id);}
interface TransportDriverProfiles extends TransportRepository<TransportDriverProfile> {Optional<TransportDriverProfile> findByDriver_Id(Long id);Optional<TransportDriverProfile> findByAccount_Id(Long id);}
interface TransportOdometers extends TransportRepository<TransportOdometer> {List<TransportOdometer> findByVehicle_IdOrderByReadingDateAscCreatedAtAsc(Long id);}
interface TransportDocuments extends TransportRepository<TransportDocument> {List<TransportDocument> findByVehicle_IdOrderByCreatedAtDesc(Long id);List<TransportDocument> findByActiveTrueAndArchivedAtIsNull();}
interface TransportComponents extends TransportRepository<TransportComponent> {List<TransportComponent> findByVehicle_IdAndArchivedAtIsNull(Long id);}
interface TransportFaults extends TransportRepository<TransportFault> {Optional<TransportFault> findByRequestKey(String key);List<TransportFault> findByVehicle_IdAndArchivedAtIsNull(Long id);}
interface TransportWorkOrders extends TransportRepository<TransportWorkOrder> {List<TransportWorkOrder> findByVehicle_IdAndArchivedAtIsNull(Long id);List<TransportWorkOrder> findByFault_IdAndArchivedAtIsNull(Long id);}
interface TransportInspectionTemplates extends TransportRepository<TransportInspectionTemplate> {}
interface TransportInspectionItems extends TransportRepository<TransportInspectionItem> {List<TransportInspectionItem> findByTemplate_IdOrderByPositionAsc(Long id);}
interface TransportInspectionAssignments extends TransportRepository<TransportInspectionAssignment> {List<TransportInspectionAssignment> findByVehicle_IdAndActiveTrue(Long id);}
interface TransportInspections extends TransportRepository<TransportInspection> {Optional<TransportInspection> findByRequestKey(String key);List<TransportInspection> findByVehicle_IdAndArchivedAtIsNull(Long id);}
interface TransportInspectionResults extends TransportRepository<TransportInspectionResult> {List<TransportInspectionResult> findByInspection_Id(Long id);}
interface TransportProcurements extends TransportRepository<TransportProcurement> {Optional<TransportProcurement> findByRequestKey(String key);List<TransportProcurement> findByVehicle_IdAndArchivedAtIsNull(Long id);List<TransportProcurement> findByAssignedTo_IdAndArchivedAtIsNull(Long id);}
interface TransportProcurementItems extends TransportRepository<TransportProcurementItem> {List<TransportProcurementItem> findByRequest_Id(Long id);}
interface TransportAssets extends TransportRepository<TransportAsset> {Optional<TransportAsset> findByRequestKey(String key);
 @Query("select (count(v)>0) from FileVersion v where v.storageKey=:key and (exists (select a.id from TransportAsset a where a.file.id=v.item.id) or exists (select d.id from TransportDocument d where d.file.id=v.item.id) or exists (select t.id from Attachment t where t.managedFile.id=v.item.id))") boolean storedKey(@Param("key")String key);List<TransportAsset> findByVehicle_IdAndRemovedFalse(Long id);List<TransportAsset> findByFuel_IdAndRemovedFalse(Long id);}
interface TransportFuelDetails extends TransportRepository<TransportFuelDetail> {Optional<TransportFuelDetail> findByRequestKey(String key);Optional<TransportFuelDetail> findByFuel_Id(Long id);List<TransportFuelDetail> findByVehicle_Id(Long id);}
interface TransportMaintenanceDetails extends TransportRepository<TransportMaintenanceDetail> {Optional<TransportMaintenanceDetail> findByMaintenance_Id(Long id);}
interface TransportTimelines extends TransportRepository<TransportTimeline> {List<TransportTimeline> findByVehicle_IdOrderByCreatedAtDesc(Long id);}
interface TransportNotifications extends TransportRepository<TransportNotification> {
 boolean existsByDedupeKey(String key);
 List<TransportNotification> findByRecipient_IdOrderByCreatedAtDesc(Long id);
 @Query("select n from TransportNotification n where n.emailEnabled=true and (:emailOn=true or n.eventType='SMTP_TEST') and n.attempts<5 and ((n.deliveryStatus in ('PENDING','RETRY') and n.nextAttemptAt<=:now) or (n.deliveryStatus='SENDING' and n.leaseUntil<:now)) order by n.createdAt") List<TransportNotification> due(@Param("now")Instant now,@Param("emailOn")boolean emailOn,org.springframework.data.domain.Pageable page);
}
interface TransportPolicies extends TransportRepository<TransportPolicy> {Optional<TransportPolicy> findByScope(String scope);}

interface TransportPartReceipts extends TransportRepository<TransportPartReceipt> {List<TransportPartReceipt> findByRequest_Id(Long id);List<TransportPartReceipt> findByBatchKey(String key);}

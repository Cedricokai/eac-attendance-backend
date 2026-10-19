package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.datasource.url=jdbc:h2:mem:inventory-test;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.jpa.show-sql=false","spring.flyway.enabled=false","spring.liquibase.enabled=false","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
class InventoryPurchasePersistenceTest {
 @Autowired EntityManager em;
 @Autowired InventoryPurchaseLineRepository lines;
 @Autowired InventoryRequestRepository requests;
 @Autowired InventoryProcurementProposalRepository proposals;
 @Test void receivedGoodsStayReservedUntilEmployeeHandover(){
  InventoryPurchaseLine a=new InventoryPurchaseLine();a.setInventoryRequestId(1L);a.setRequestItemId(2L);a.setProductId(3L);a.setRoute("PETTY_CASH");a.setQuantity(6);a.setUnitCost(BigDecimal.TEN);a.setReceivedQuantity(6);a.setIssuedQuantity(2);lines.saveAndFlush(a);
  InventoryPurchaseLine b=new InventoryPurchaseLine();b.setInventoryRequestId(4L);b.setProductId(3L);b.setQuantity(5);b.setReceivedQuantity(3);lines.saveAndFlush(b);
  assertEquals(7,lines.reservedStock(3L));a.setIssuedQuantity(6);lines.saveAndFlush(a);assertEquals(3,lines.reservedStock(3L));assertEquals(0,lines.reservedStock(999L));assertEquals(1,lines.findByInventoryRequestIdOrderById(1L).size());
 }
 @Test void proposalReservationsAreReleasedWhenReturnedOrFunded(){
  InventoryProcurementProposal p=new InventoryProcurementProposal();p.setInventoryRequestId(1L);p.setReference("IPP-TEST");p.setSupplier("Supplier");p.setQuotationReference("QUOTE-1");p.setTotalAmount(BigDecimal.TEN);InventoryProposalItem i=new InventoryProposalItem();i.setProposal(p);i.setRequestItemId(2L);i.setQuantity(6);i.setUnitCost(BigDecimal.TEN);p.getItems().add(i);proposals.saveAndFlush(p);assertEquals(6,proposals.pendingQuantity(1L,2L));p.setStatus("RETURNED");proposals.saveAndFlush(p);assertEquals(0,proposals.pendingQuantity(1L,2L));p.setStatus("AWAITING_ACCOUNTS");proposals.saveAndFlush(p);assertEquals(6,proposals.pendingQuantity(1L,2L));p.setStatus("FUNDED");proposals.saveAndFlush(p);assertEquals(0,proposals.pendingQuantity(1L,2L));
 }
 @Test void requestLockPreservesStableEmployeeIdentity(){
  InventoryRequest r=new InventoryRequest();r.setRequestNumber("IR-LOCK");r.setRequesterUserId(10L);r.setRequesterEmail("employee@example.test");r.setFinanceFulfilment(true);r.setStatus(RequestStatus.PARTIALLY_ISSUED);requests.saveAndFlush(r);em.clear();InventoryRequest saved=requests.findForUpdate(r.getId()).orElseThrow();assertEquals(10L,saved.getRequesterUserId());assertTrue(saved.isFinanceFulfilment());assertEquals(RequestStatus.PARTIALLY_ISSUED,saved.getStatus());
 }
}

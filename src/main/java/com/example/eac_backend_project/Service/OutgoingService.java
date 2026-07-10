package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.inventory.Outgoing;
import com.example.eac_backend_project.entity.inventory.OutgoingRecordType;
import com.example.eac_backend_project.repository.OutgoingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class OutgoingService {
    private static final Logger logger = LoggerFactory.getLogger(OutgoingService.class);

    @Autowired
    private final OutgoingRepository outgoingRepository;

    public OutgoingService(OutgoingRepository outgoingRepository) {
        this.outgoingRepository = outgoingRepository;
    }

    public List<Outgoing> getAllOutGoingProducts() {
        return outgoingRepository.findAll();
    }

    public List<Outgoing> getOutgoingByType(OutgoingRecordType type) {
        return outgoingRepository.findByRecordType(type);
    }

    public List<Outgoing> getOutgoingByRequestId(Long requestId) {
        return outgoingRepository.findByRequestId(requestId);
    }

    public List<Outgoing> getOutgoingByProductId(Long productId) {
        return outgoingRepository.findByProductId(productId);
    }

    public List<Outgoing> getOutgoingByUserName(String userName) {
        return outgoingRepository.findByUserName(userName);
    }

    @Transactional
    public List<Outgoing> saveAllOutgoing(List<Outgoing> outgoingItems) {
        for (Outgoing item : outgoingItems) {
            logger.info("Batch saving outgoing: name='{}', type='{}', quantityMoved={}, productId={}, location='{}'",
                    item.getName(), item.getRecordType(), item.getQuantityMoved(),
                    item.getProductId(), item.getLocation());
        }
        List<Outgoing> savedItems = outgoingRepository.saveAll(outgoingItems);
        logger.info("Successfully saved {} outgoing items to database", savedItems.size());
        return savedItems;
    }

    public Long getTotalOutgoingProducts(){
        return outgoingRepository.count();
    }

    public List<Outgoing> getOutgoingByStoreType(String storeType) {
        return outgoingRepository.findByStoreType(storeType);
    }

    public Outgoing findById(Long id) {
        Optional<Outgoing> outgoing = outgoingRepository.findById(id);
        return outgoing.orElse(null);
    }

    @Transactional
    public Outgoing saveOutgoing(Outgoing outgoingItem) {
        logger.info("Saving outgoing: name='{}', type='{}', storeType='{}', returnable={}",
                outgoingItem.getName(), outgoingItem.getRecordType(),
                outgoingItem.getStoreType(), outgoingItem.getReturnableAfterUse());
        return outgoingRepository.save(outgoingItem);
    }


}
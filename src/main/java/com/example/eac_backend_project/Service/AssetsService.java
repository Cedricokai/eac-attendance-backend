package com.example.eac_backend_project.Service;


import com.example.eac_backend_project.config.Assets;
import com.example.eac_backend_project.entity.inventory.ActionType;
import com.example.eac_backend_project.entity.inventory.Outgoing;
import com.example.eac_backend_project.entity.inventory.ProductHistory;
import com.example.eac_backend_project.repository.AssetsRepository;
import com.example.eac_backend_project.repository.OutgoingRepository;
import com.example.eac_backend_project.repository.ProductHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssetsService {

    private final AssetsRepository assetsRepository;
    private final OutgoingRepository outgoingRepository;
    private final ProductHistoryRepository productHistoryRepository;
    private final Logger logger = LoggerFactory.getLogger(AssetsService.class);
    
    public AssetsService(AssetsRepository assetsRepository,  OutgoingRepository outgoingRepository, ProductHistoryRepository productHistoryRepository) {
        this.assetsRepository = assetsRepository;
        this.outgoingRepository = outgoingRepository;
        this.productHistoryRepository = productHistoryRepository;
    }

    public List<Assets> getAllAssets() {
        return assetsRepository.findAll();
    }

    public Assets addAssets(Assets assets,  String userEmail) {
        Assets savedAssets = assetsRepository.save(assets);
        logProductHistory(savedAssets, ActionType.ADD, userEmail);
        logger.info("Product added: {}", savedAssets.getName());
        return savedAssets;
    }

    public Assets updateAssets(Long id, Assets updatedAssets, String userEmail) {
        Optional<Assets> existingAssetsOptional = assetsRepository.findById(id);
        if (existingAssetsOptional.isPresent()) {
            Assets existingAssets = existingAssetsOptional.get();

            // Update fields
            existingAssets.setName( updatedAssets.getName());
             existingAssets.setCode( updatedAssets.getCode());
             existingAssets.setDescription( updatedAssets.getDescription());
             existingAssets.setStock( updatedAssets.getStock());
             existingAssets.setUserName( updatedAssets.getUserName());
             existingAssets.setProductType( updatedAssets.getProductType());

            // Save the updated product
            Assets savedAssets = assetsRepository.save( existingAssets);

            // Log the action in product history
            logProductHistory(savedAssets, ActionType.EDIT, userEmail);

            return savedAssets;
        } else {
            throw new RuntimeException("Product with ID " + id + " not found.");
        }
    }

    public boolean existsById(Long id) {
        return assetsRepository.existsById(id);
    }

    public void deleteAssets(Long id, String userEmail) {
        Optional<Assets> assetsOptional = assetsRepository.findById(id);
        if (assetsOptional.isPresent()) {
            Assets assets = assetsOptional.get();
            logProductHistory(assets, ActionType.DELETE, userEmail);
            assetsRepository.deleteById(id);
        } else {
            throw new RuntimeException("Asset Product with ID " + id + " not found"); // Asset not found
        }
    }

    public List<Assets> searchAssets(String query) {
        return assetsRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(query, query);
    }

     @Transactional
    public void moveProductsToOutgoing(List<Long> assetsIds, String location) {
        List<Assets> assetsToMove = assetsRepository.findAllById(assetsIds);
        logger.info("Products to move: {}", assetsToMove);

        if ( assetsToMove.isEmpty()) {
            throw new IllegalArgumentException("No products found for the given IDs.");
        }

        assetsToMove.forEach(assets -> {
            assets.setStock(assets.getStock() - 1);
            assetsRepository.save(assets);
        });

        List<Outgoing> outgoingItems = assetsToMove.stream().map(assets -> {
            Outgoing outgoing = new Outgoing();
            outgoing.setName(assets.getName());
            outgoing.setCode( assets.getCode());
            outgoing.setDescription( assets.getDescription());
            outgoing.setMovementDate( assets.getLastUpdatedDate());
            outgoing.setLocation(location);
            logger.info("Creating outgoing item: {}", outgoing);
            return outgoing;
        }).collect(Collectors.toList());

        outgoingRepository.saveAll(outgoingItems);
    }

     private void logProductHistory(Assets assets, ActionType actionType, String userEmail) {
        ProductHistory history = new ProductHistory();
        history.setAssetsId(assets.getId());
        history.setActionType(actionType);
        history.setActionDate(LocalDateTime.now());
        history.setUserEmail(userEmail);
        history.setDescription("The Action '" + actionType.name() + "' was performed on the product '" + assets.getName() + "'");

        // Save the history record
        productHistoryRepository.save(history);
    }
}

package com.example.eac_backend_project.Service;


import com.example.eac_backend_project.entity.inventory.Outgoing;
import com.example.eac_backend_project.repository.OutgoingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


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

    public void saveOutgoing(Outgoing outgoingItem) {
        outgoingRepository.save(outgoingItem);
    }





}
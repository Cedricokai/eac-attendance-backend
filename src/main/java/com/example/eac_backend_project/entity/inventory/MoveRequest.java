package com.example.eac_backend_project.entity.inventory;

import lombok.Data;
import java.util.Map;

@Data
public class MoveRequest {
        private Map<Long, Integer> productQuantities; // Product ID -> Quantity to move
        private String location;
        private String requestedBy;

        // Constructors
        public MoveRequest() {}

        public MoveRequest(Map<Long, Integer> productQuantities, String location, String requestedBy) {
                this.productQuantities = productQuantities;
                this.location = location;
                this.requestedBy = requestedBy;
        }

        // Getters and Setters
        public Map<Long, Integer> getProductQuantities() {
                return productQuantities;
        }

        public void setProductQuantities(Map<Long, Integer> productQuantities) {
                this.productQuantities = productQuantities;
        }

        public String getLocation() {
                return location;
        }

        public void setLocation(String location) {
                this.location = location;
        }

        public String getRequestedBy() {
                return requestedBy;
        }

        public void setRequestedBy(String requestedBy) {
                this.requestedBy = requestedBy;
        }
}
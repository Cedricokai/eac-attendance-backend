package com.example.eac_backend_project.entity.inventory;

import lombok.Data;

import java.util.List;

@Data
public class MoveRequest {


        private List<Long> productIds;
        private List<Long> assetsIds;
        private String location;
        private String userName;
        private String requestedBy;

        public List<Long> getProductIds() {
                return productIds;
        }

        public void setProductIds(List<Long> productIds) {
                this.productIds = productIds;
        }

        public List<Long> getAssetsIds() {
                return assetsIds;
        }

        public void setAssetsIds(List<Long> assetsIds) {
                this.assetsIds = assetsIds;
        }

        public String getLocation() {
                return location;
        }

        public void setLocation(String location) {
                this.location = location;
        }

        public String getUserName() {
                return userName;
        }

        public void setUserName(String userName) {
                this.userName = userName;
        }

        public String getRequestedBy() {
                return requestedBy;
        }

        public void setRequestedBy(String requestedBy) {
                this.requestedBy = requestedBy;
        }

        // Getters and Setters

}

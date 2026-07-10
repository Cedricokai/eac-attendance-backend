package com.example.eac_backend_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeBulkDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String jobPosition;
    private String jobGrade;
    private String category;
    private Double minimumRate;
    private String workType;
    private String ssnitAccountName;
    private String ssnitNumber;
    private String accountName;
    private String tinNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double basicSalary;
    private String accountNumber;
    private String tagNumber;
    private LocalDate dateOfBirth;
    private String emergencyContact;
    private String department;
    private Double rentAllowance;
    private Double transportAllowance;
    private Double clothingAllowance;
    private Double otherAllowance;
    private Double nssAllowance;
    private String location;
    private String ghanaCard;
    private String bank;
    private String bankBranch;
    private String contactPerson;
    private String relationship;
    private String townOfResidence;
    private String houseNumber;
    private String spouse;
    private Integer numberOfChildren;
    private Integer age;
    private String originalEmployeeId;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobPosition() {
        return jobPosition;
    }

    public void setJobPosition(String jobPosition) {
        this.jobPosition = jobPosition;
    }

    public String getJobGrade() {
        return jobGrade;
    }

    public void setJobGrade(String jobGrade) {
        this.jobGrade = jobGrade;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getMinimumRate() {
        return minimumRate;
    }

    public void setMinimumRate(Double minimumRate) {
        this.minimumRate = minimumRate;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getSsnitNumber() {
        return ssnitNumber;
    }

    public void setSsnitNumber(String ssnitNumber) {
        this.ssnitNumber = ssnitNumber;
    }

    public String getTinNumber() {
        return tinNumber;
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = tinNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getTagNumber() {
        return tagNumber;
    }

    public void setTagNumber(String tagNumber) {
        this.tagNumber = tagNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Double getRentAllowance() {
        return rentAllowance;
    }

    public void setRentAllowance(Double rentAllowance) {
        this.rentAllowance = rentAllowance;
    }

    public Double getTransportAllowance() {
        return transportAllowance;
    }

    public void setTransportAllowance(Double transportAllowance) {
        this.transportAllowance = transportAllowance;
    }

    public Double getClothingAllowance() {
        return clothingAllowance;
    }

    public void setClothingAllowance(Double clothingAllowance) {
        this.clothingAllowance = clothingAllowance;
    }

    public Double getOtherAllowance() {
        return otherAllowance;
    }

    public void setOtherAllowance(Double otherAllowance) {
        this.otherAllowance = otherAllowance;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGhanaCard() {
        return ghanaCard;
    }

    public void setGhanaCard(String ghanaCard) {
        this.ghanaCard = ghanaCard;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getTownOfResidence() {
        return townOfResidence;
    }

    public void setTownOfResidence(String townOfResidence) {
        this.townOfResidence = townOfResidence;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getSpouse() {
        return spouse;
    }

    public void setSpouse(String spouse) {
        this.spouse = spouse;
    }

    public Integer getNumberOfChildren() {
        return numberOfChildren;
    }

    public void setNumberOfChildren(Integer numberOfChildren) {
        this.numberOfChildren = numberOfChildren;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getOriginalEmployeeId() {
        return originalEmployeeId;
    }

    public void setOriginalEmployeeId(String originalEmployeeId) {
        this.originalEmployeeId = originalEmployeeId;
    }
}

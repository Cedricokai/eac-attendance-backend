package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.attendance.Job;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class JobDTO {
    private Long id;
    private String name;
    private String description;
    private String client;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Job.BillingCycle billingCycle;
    private Double budget;
    private Double hourlyRate;
    private Integer standardWorkHours;
    private Job.JobStatus status;
    private List<EmployeeDTO> assignedEmployees;
    private Integer employeeCount;

    // ========== ADD THESE MISSING BILLING REMINDER FIELDS ==========
    private boolean billingReminderEnabled;
    private String billingReminderEmails;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextBillingDate;

    private Boolean billOn15NextMonth;
    private Integer billingAnchorDay;

    // Reminder configuration fields
    private Boolean enableAdvanceReminders;
    private String reminderDaysBefore;
    private Integer reminderTimeHour;
    private Integer reminderTimeMinute;
    private Boolean enableOverdueReminders;
    private Integer overdueReminderDays;

    // Supervisor info
    private EmployeeDTO supervisor;

    // Default constructor
    public JobDTO() {}

    // Constructor with Job parameter - ADD ALL FIELDS
    public JobDTO(Job job) {
        this.id = job.getId();
        this.name = job.getName();
        this.description = job.getDescription();
        this.client = job.getClient();
        this.startDate = job.getStartDate();
        this.endDate = job.getEndDate();
        this.billingCycle = job.getBillingCycle();
        this.budget = job.getBudget();
        this.hourlyRate = job.getHourlyRate();
        this.standardWorkHours = job.getStandardWorkHours();
        this.status = job.getStatus();

        // ========== ADD THESE BILLING REMINDER FIELDS ==========
        this.billingReminderEnabled = job.isBillingReminderEnabled();
        this.billingReminderEmails = job.getBillingReminderEmails();
        this.nextBillingDate = job.getNextBillingDate();
        this.billOn15NextMonth = job.getBillOn15NextMonth();
        this.billingAnchorDay = job.getBillingAnchorDay();

        this.enableAdvanceReminders = job.getEnableAdvanceReminders();
        this.reminderDaysBefore = job.getReminderDaysBefore();
        this.reminderTimeHour = job.getReminderTimeHour();
        this.reminderTimeMinute = job.getReminderTimeMinute();
        this.enableOverdueReminders = job.getEnableOverdueReminders();
        this.overdueReminderDays = job.getOverdueReminderDays();

        // Add supervisor if exists
        if (job.getSupervisor() != null) {
            EmployeeDTO supervisorDTO = new EmployeeDTO();
            supervisorDTO.setId(job.getSupervisor().getId());
            supervisorDTO.setFirstName(job.getSupervisor().getFirstName());
            supervisorDTO.setLastName(job.getSupervisor().getLastName());
            supervisorDTO.setEmployeeId(job.getSupervisor().getEmployeeId());
            supervisorDTO.setEmail(job.getSupervisor().getEmail());
            this.supervisor = supervisorDTO;
        }

        // Map employees to EmployeeDTO with ALL fields
        this.assignedEmployees = job.getEmployees().stream()
                .map(employee -> {
                    EmployeeDTO dto = new EmployeeDTO();
                    dto.setId(employee.getId());
                    dto.setEmployeeId(employee.getEmployeeId());
                    dto.setFirstName(employee.getFirstName());
                    dto.setLastName(employee.getLastName());
                    dto.setEmail(employee.getEmail());
                    dto.setPhone(employee.getPhone());
                    dto.setJobPosition(employee.getJobPosition());
                    dto.setJobGrade(employee.getJobGrade());
                    dto.setCategory(employee.getCategory() != null ? employee.getCategory().getName() : null);
                    dto.setMinimumRate(employee.getMinimumRate());
                    dto.setWorkType(employee.getWorkType());
                    dto.setSsnitNumber(employee.getSsnitNumber());
                    dto.setTinNumber(employee.getTinNumber());
                    dto.setTagNumber(employee.getTagNumber());
                    dto.setDateOfBirth(employee.getDateOfBirth());
                    dto.setLocation(employee.getLocation());
                    dto.setGhanaCard(employee.getGhanaCard());
                    dto.setBank(employee.getBank());
                    dto.setBankBranch(employee.getBankBranch());
                    dto.setContactPerson(employee.getContactPerson());
                    dto.setRelationship(employee.getRelationship());
                    dto.setTownOfResidence(employee.getTownOfResidence());
                    dto.setHouseNumber(employee.getHouseNumber());
                    dto.setSpouse(employee.getSpouse());
                    dto.setNumberOfChildren(employee.getNumberOfChildren());
                    dto.setAge(employee.getAge());
                    dto.setUsePositionRate(employee.getUsePositionRate());
                    dto.setBasicSalary(employee.getBasicSalary());
                    dto.setStartDate(employee.getStartDate());
                    dto.setEndDate(employee.getEndDate());
                    dto.setAccountNumber(employee.getAccountNumber());
                    dto.setEmergencyContact(employee.getEmergencyContact());
                    dto.setDepartment(employee.getDepartment());
                    dto.setRentAllowance(employee.getRentAllowance());
                    dto.setTransportAllowance(employee.getTransportAllowance());
                    dto.setClothingAllowance(employee.getClothingAllowance());
                    dto.setOtherAllowance(employee.getOtherAllowance());
                    dto.setIsSupervisor(employee.getIsSupervisor());
                    dto.setActive(employee.getActive());

                    if (employee.getJob() != null) {
                        dto.setJobId(employee.getJob().getId());
                        dto.setJobName(employee.getJob().getName());
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        this.employeeCount = this.assignedEmployees != null ? this.assignedEmployees.size() : 0;
    }

    // ========== ADD ALL GETTERS AND SETTERS ==========
    public boolean isBillingReminderEnabled() {
        return billingReminderEnabled;
    }

    public void setBillingReminderEnabled(boolean billingReminderEnabled) {
        this.billingReminderEnabled = billingReminderEnabled;
    }

    public String getBillingReminderEmails() {
        return billingReminderEmails;
    }

    public void setBillingReminderEmails(String billingReminderEmails) {
        this.billingReminderEmails = billingReminderEmails;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(LocalDate nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public Boolean getBillOn15NextMonth() {
        return billOn15NextMonth;
    }

    public void setBillOn15NextMonth(Boolean billOn15NextMonth) {
        this.billOn15NextMonth = billOn15NextMonth;
    }

    public Integer getBillingAnchorDay() {
        return billingAnchorDay;
    }

    public void setBillingAnchorDay(Integer billingAnchorDay) {
        this.billingAnchorDay = billingAnchorDay;
    }

    public Boolean getEnableAdvanceReminders() {
        return enableAdvanceReminders;
    }

    public void setEnableAdvanceReminders(Boolean enableAdvanceReminders) {
        this.enableAdvanceReminders = enableAdvanceReminders;
    }

    public String getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore(String reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }

    public Integer getReminderTimeHour() {
        return reminderTimeHour;
    }

    public void setReminderTimeHour(Integer reminderTimeHour) {
        this.reminderTimeHour = reminderTimeHour;
    }

    public Integer getReminderTimeMinute() {
        return reminderTimeMinute;
    }

    public void setReminderTimeMinute(Integer reminderTimeMinute) {
        this.reminderTimeMinute = reminderTimeMinute;
    }

    public Boolean getEnableOverdueReminders() {
        return enableOverdueReminders;
    }

    public void setEnableOverdueReminders(Boolean enableOverdueReminders) {
        this.enableOverdueReminders = enableOverdueReminders;
    }

    public Integer getOverdueReminderDays() {
        return overdueReminderDays;
    }

    public void setOverdueReminderDays(Integer overdueReminderDays) {
        this.overdueReminderDays = overdueReminderDays;
    }

    public EmployeeDTO getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(EmployeeDTO supervisor) {
        this.supervisor = supervisor;
    }
}
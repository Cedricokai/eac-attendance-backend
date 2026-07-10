package com.example.eac_backend_project.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ComparisonSummaryDTO {
    private Long id;
    private Summary summary;
    private List<EmployeeComparisonDTO> employees;
    private List<UnmatchedEmployeeDTO> unmatchedA;
    private List<UnmatchedEmployeeDTO> unmatchedB;
    private Map<String, List<UnknownCodeDTO>> unknownCodes;
    private Map<String, Integer> employeesWithMostDisparities;

    @Data
    public static class Summary {
        private Integer totalEmployees;
        private Integer totalDates;
        private Integer matches;
        private Integer disparities;
        private DisparityBreakdown byDisparityType;
        private Map<String, CodeSummary> byCode;
    }

    @Data
    public static class DisparityBreakdown {
        private Integer hoursMismatch;
        private Integer noCode;
        private Integer noHours;
        private Integer unknownCode;
        private Integer weekendPattern;
    }

    @Data
    public static class CodeSummary {
        private Integer count;
        private Integer matches;
        private Integer mismatches;
    }

    @Data
    public static class EmployeeComparisonDTO {
        private String name;
        private String id;
        private String matchType;
        private Map<String, DateComparisonDTO> dates;
        private EmployeeSummary summary;
    }

    @Data
    public static class EmployeeSummary {
        private Integer totalDays;
        private Integer daysWithData;
        private Integer matches;
        private Integer disparities;
        private Double totalHoursDiff;
    }

    @Data
    public static class DateComparisonDTO {
        private String date;
        private Double hoursValue;
        private Object hoursOriginal;
        private String codeValue;
        private Object codeOriginal;
        private Integer expectedHours;
        private String disparityType; // 'match', 'hoursMismatch', 'noCode', 'noHours', 'unknownCode'
        private Map<String, Object> disparityDetails;
    }

    @Data
    public static class UnmatchedEmployeeDTO {
        private String name;
        private String id;
        private Integer rowNumber;
    }

    @Data
    public static class UnknownCodeDTO {
        private String employee;
        private String date;
        private Object value;
    }
}
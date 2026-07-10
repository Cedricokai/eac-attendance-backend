package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.ComparisonSummaryDTO;
import com.example.eac_backend_project.entity.attendance.Comparison;
import com.example.eac_backend_project.entity.attendance.RuleSet;
import com.example.eac_backend_project.entity.attendance.RuleSetItem;
import com.example.eac_backend_project.repository.ComparisonRepository;
import com.example.eac_backend_project.repository.RuleSetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComparisonService {

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ComparisonRepository comparisonRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public ComparisonSummaryDTO compareFiles(
            MultipartFile fileA,
            MultipartFile fileB,
            String sheetA,
            String sheetB,
            Long ruleSetId,
            String username) {

        try {
            System.out.println("Starting comparison...");
            System.out.println("File A: " + fileA.getOriginalFilename() + ", Sheet: " + sheetA);
            System.out.println("File B: " + fileB.getOriginalFilename() + ", Sheet: " + sheetB);

            // Auto-detect name columns or use defaults
            Integer nameColumnA = detectNameColumn(fileA, sheetA);
            Integer nameColumnB = detectNameColumn(fileB, sheetB);

            System.out.println("Using name column for File A: " + nameColumnA);
            System.out.println("Using name column for File B: " + nameColumnB);

            ParsedFileData dataA = parseExcelFile(fileA, sheetA, nameColumnA);
            ParsedFileData dataB = parseExcelFile(fileB, sheetB, nameColumnB);

            System.out.println("File A employees found: " + dataA.employees.size());
            System.out.println("File B employees found: " + dataB.employees.size());

            RuleSet ruleSet = ruleSetRepository.findById(ruleSetId)
                    .orElseThrow(() -> new RuntimeException("Rule set not found"));

            // Print first few employees from each file for debugging
            System.out.println("\n--- Sample employees from File A ---");
            dataA.employees.stream().limit(5).forEach(e -> System.out.println("  " + e.getName()));

            System.out.println("\n--- Sample employees from File B ---");
            dataB.employees.stream().limit(5).forEach(e -> System.out.println("  " + e.getName()));

            // Match employees by name only (case insensitive, trimmed)
            EmployeeMatchingResult matchingResult = matchEmployeesByName(dataA.employees, dataB.employees);

            System.out.println("\nMatched employees: " + matchingResult.matched.size());
            System.out.println("Unmatched in A: " + matchingResult.unmatchedA.size());
            System.out.println("Unmatched in B: " + matchingResult.unmatchedB.size());

            ComparisonSummaryDTO result = performComparison(
                    matchingResult,
                    ruleSet,
                    dataA.dateColumnMap,
                    dataB.dateColumnMap);

            saveComparisonToDatabase(fileA, fileB, sheetA, sheetB, ruleSet, result, username);

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error comparing files: " + e.getMessage(), e);
        }
    }

    /**
     * Detect which column contains employee names
     * For File B (Timesheet), force column B (index 1)
     */
    private Integer detectNameColumn(MultipartFile file, String sheetName) {
        String filename = file.getOriginalFilename().toLowerCase();

        // For Timesheet file (codes), always use column B (index 1)
        if (filename.contains("timesheet") || filename.contains("feb") || filename.contains("codes")) {
            System.out.println("Detected codes file, using column B (index 1) for names");
            return 1; // Column B
        }

        // For NPI file (hours), try to auto-detect but default to column B
        if (filename.contains("npi") || filename.contains("hours")) {
            System.out.println("Detected hours file, will auto-detect name column");
            return null; // Let parseExcelFile auto-detect
        }

        return null; // Auto-detect for other files
    }

    private ParsedFileData parseExcelFile(MultipartFile file, String sheetName, Integer forcedNameColumn) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
            System.out.println("Sheet not found, using first sheet: " + sheet.getSheetName());
        }

        List<EmployeeData> employees = new ArrayList<>();
        Map<Integer, Date> dateColumnMap = new HashMap<>();

        int headerRowIndex = -1;
        int nameColumn = -1;

        // STEP 1: Find the header row (could be any row)
        System.out.println("Searching for header row with dates...");
        for (int i = 0; i <= Math.min(30, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            int possibleDateCount = 0;

            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                if (cell == null) continue;

                // Check for actual Date cells
                if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    possibleDateCount++;
                }
                // Check for string that looks like a date (YYYY-MM-DD)
                else if (cell.getCellType() == CellType.STRING) {
                    String value = cell.getStringCellValue().trim();
                    if (value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                        possibleDateCount++;
                    }
                }
            }

            if (possibleDateCount >= 3) {
                headerRowIndex = i;
                System.out.println("Found header row at index: " + i + " with " + possibleDateCount + " dates");
                break;
            }
        }

        // If still not found, try to find by scanning for date patterns in data rows
        if (headerRowIndex == -1) {
            System.out.println("No explicit header row found, scanning data rows for dates...");
            for (int i = 0; i <= Math.min(20, sheet.getLastRowNum()); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                            headerRowIndex = Math.max(0, i - 1);
                            System.out.println("Found date at row " + i + ", assuming header at row " + headerRowIndex);
                            break;
                        }
                    }
                }
                if (headerRowIndex != -1) break;
            }
        }

        // Last resort - assume first row is header
        if (headerRowIndex == -1) {
            headerRowIndex = 0;
            System.out.println("No header row found, assuming row 0 is header");
        }

        // STEP 2: Find the name column
        // If forcedNameColumn is provided, use it
        if (forcedNameColumn != null) {
            nameColumn = forcedNameColumn;
            System.out.println("Using forced name column: " + nameColumn);
        } else {
            // Auto-detect name column
            Row headerRow = sheet.getRow(headerRowIndex);

            // First, try to find column with "name" header
            if (headerRow != null) {
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    Cell cell = headerRow.getCell(j);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue().toLowerCase().trim();
                        if (value.contains("name") || value.contains("employee") || value.contains("staff")) {
                            nameColumn = j;
                            System.out.println("Found name column at index " + j + " with header: " + value);
                            break;
                        }
                    }
                }
            }

            // If no name header, check data rows to find column with most text entries
            if (nameColumn == -1) {
                Map<Integer, Integer> textCount = new HashMap<>();
                Map<Integer, Integer> namePatternCount = new HashMap<>();

                int startRow = headerRowIndex + 1;
                int endRow = Math.min(startRow + 30, sheet.getLastRowNum());

                for (int i = startRow; i <= endRow; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null && cell.getCellType() == CellType.STRING) {
                            String value = cell.getStringCellValue().trim();
                            if (!value.isEmpty() && !value.matches("^\\d+$") && value.length() > 2) {
                                textCount.put(j, textCount.getOrDefault(j, 0) + 1);

                                // Check for name patterns (contains spaces, proper case, etc.)
                                if (value.contains(" ") && value.matches(".*[A-Z].*")) {
                                    namePatternCount.put(j, namePatternCount.getOrDefault(j, 0) + 1);
                                }
                            }
                        }
                    }
                }

                // Prefer columns that have name patterns
                if (!namePatternCount.isEmpty()) {
                    int maxNamePattern = 0;
                    for (Map.Entry<Integer, Integer> entry : namePatternCount.entrySet()) {
                        if (entry.getValue() > maxNamePattern) {
                            maxNamePattern = entry.getValue();
                            nameColumn = entry.getKey();
                        }
                    }
                }
                // Otherwise use column with most text
                else if (!textCount.isEmpty()) {
                    int maxCount = 0;
                    for (Map.Entry<Integer, Integer> entry : textCount.entrySet()) {
                        if (entry.getValue() > maxCount) {
                            maxCount = entry.getValue();
                            nameColumn = entry.getKey();
                        }
                    }
                }
            }
        }

        // Default to column B (index 1) if still not found
        if (nameColumn == -1) {
            nameColumn = 1;
            System.out.println("Name column not found, defaulting to column B (index 1)");
        }

        // STEP 3: Parse date columns from header row
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow != null) {
            for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                if (j == nameColumn) continue;

                Cell cell = headerRow.getCell(j);
                if (cell != null) {
                    Date date = parseDateFromCell(cell);
                    if (date != null) {
                        dateColumnMap.put(j, date);
                        System.out.println("Found date at column " + j + ": " + toYMD(date));
                    }
                }
            }
        }

        System.out.println("Found " + dateColumnMap.size() + " date columns");

        // STEP 4: Parse employee data (starting after header row)
        int dataStartRow = headerRowIndex + 1;
        for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            // Get employee name from the identified name column
            Cell nameCell = row.getCell(nameColumn);
            if (nameCell == null) continue;

            String name = getStringCellValue(nameCell).trim();

            // Skip empty rows, totals, summaries
            if (name.isEmpty() || name.toLowerCase().contains("total") ||
                    name.toLowerCase().contains("summary") || name.toLowerCase().contains("subtotal") ||
                    name.toLowerCase().contains("normal days") || name.matches("^\\d+$")) {
                continue;
            }

            EmployeeData emp = new EmployeeData();
            emp.setName(name);
            emp.setRowNumber(i + 1);

            // Parse attendance data for each date column
            for (Map.Entry<Integer, Date> entry : dateColumnMap.entrySet()) {
                int col = entry.getKey();
                Date date = entry.getValue();
                String dateStr = toYMD(date);

                if (col < row.getLastCellNum()) {
                    Cell cell = row.getCell(col);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        AttendanceData attendance = new AttendanceData();
                        attendance.setDate(dateStr);
                        attendance.setOriginal(getCellValue(cell));

                        // Determine the type of data
                        if (cell.getCellType() == CellType.NUMERIC) {
                            attendance.setType("hours");
                            attendance.setValue(cell.getNumericCellValue());
                        } else if (cell.getCellType() == CellType.STRING) {
                            String strValue = cell.getStringCellValue().trim();
                            if (!strValue.isEmpty()) {
                                // Check if it's a code (short string, not a long number)
                                if (strValue.length() <= 5 && !strValue.matches("^\\d+$")) {
                                    attendance.setType("code");
                                    attendance.setValue(strValue);
                                } else {
                                    attendance.setType("other");
                                    attendance.setValue(strValue);
                                }
                            } else {
                                continue;
                            }
                        }

                        emp.getAttendance().put(dateStr, attendance);
                    }
                }
            }

            if (!emp.getAttendance().isEmpty()) {
                employees.add(emp);
                System.out.println("Added employee: " + emp.getName() + " with " + emp.getAttendance().size() + " attendance records");
            }
        }

        workbook.close();

        ParsedFileData result = new ParsedFileData();
        result.setEmployees(employees);
        result.setDateColumnMap(dateColumnMap);
        return result;
    }

    // Helper method to parse date from various cell types
    private Date parseDateFromCell(Cell cell) {
        if (cell == null) return null;

        try {
            // Case 1: It's a proper Excel date
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }

            // Case 2: It's a string that looks like a date
            if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();

                // Try YYYY-MM-DD format
                if (value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    String[] parts = value.split("-");
                    Calendar cal = Calendar.getInstance();
                    cal.set(Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]) - 1,
                            Integer.parseInt(parts[2]));
                    return cal.getTime();
                }

                // Try DD/MM/YYYY format
                if (value.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                    String[] parts = value.split("/");
                    Calendar cal = Calendar.getInstance();
                    cal.set(Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[1]) - 1,
                            Integer.parseInt(parts[0]));
                    return cal.getTime();
                }

                // Try MM/DD/YYYY format
                if (value.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                    String[] parts = value.split("/");
                    Calendar cal = Calendar.getInstance();
                    cal.set(Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[0]) - 1,
                            Integer.parseInt(parts[1]));
                    return cal.getTime();
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }

        return null;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return cell.getNumericCellValue();
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }

    private String toYMD(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private EmployeeMatchingResult matchEmployeesByName(List<EmployeeData> employeesA, List<EmployeeData> employeesB) {
        List<MatchedEmployee> matched = new ArrayList<>();
        List<EmployeeData> unmatchedA = new ArrayList<>();
        List<EmployeeData> unmatchedB = new ArrayList<>();
        Set<EmployeeData> matchedBEmployees = new HashSet<>();

        // Clean names: trim and normalize
        Map<String, EmployeeData> normalizedMapA = new HashMap<>();
        Map<String, EmployeeData> normalizedMapB = new HashMap<>();

        for (EmployeeData empA : employeesA) {
            String normalizedName = normalizeName(empA.getName());
            normalizedMapA.put(normalizedName, empA);
        }

        for (EmployeeData empB : employeesB) {
            String normalizedName = normalizeName(empB.getName());
            normalizedMapB.put(normalizedName, empB);
        }

        // Try exact matches first (case insensitive)
        for (Map.Entry<String, EmployeeData> entryA : normalizedMapA.entrySet()) {
            String nameA = entryA.getKey();
            EmployeeData empA = entryA.getValue();

            if (normalizedMapB.containsKey(nameA)) {
                EmployeeData empB = normalizedMapB.get(nameA);
                matched.add(new MatchedEmployee(empA, empB, "exact"));
                matchedBEmployees.add(empB);
                System.out.println("Exact match found: " + empA.getName() + " = " + empB.getName());
            } else {
                unmatchedA.add(empA);
            }
        }

        // Add remaining employees to unmatched lists
        for (EmployeeData empB : employeesB) {
            if (!matchedBEmployees.contains(empB)) {
                unmatchedB.add(empB);
            }
        }

        EmployeeMatchingResult result = new EmployeeMatchingResult();
        result.setMatched(matched);
        result.setUnmatchedA(unmatchedA);
        result.setUnmatchedB(unmatchedB);
        return result;
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.trim()
                .toUpperCase()
                .replaceAll("\\s+", " ")  // Replace multiple spaces with single space
                .replaceAll("[^A-Z0-9\\s]", ""); // Remove special characters
    }

    private ComparisonSummaryDTO performComparison(
            EmployeeMatchingResult matchingResult,
            RuleSet ruleSet,
            Map<Integer, Date> dateMapA,
            Map<Integer, Date> dateMapB) {

        ComparisonSummaryDTO result = new ComparisonSummaryDTO();
        ComparisonSummaryDTO.Summary summary = new ComparisonSummaryDTO.Summary();
        ComparisonSummaryDTO.DisparityBreakdown breakdown = new ComparisonSummaryDTO.DisparityBreakdown();

        breakdown.setHoursMismatch(0);
        breakdown.setNoCode(0);
        breakdown.setNoHours(0);
        breakdown.setUnknownCode(0);
        breakdown.setWeekendPattern(0);

        summary.setByDisparityType(breakdown);
        summary.setByCode(new HashMap<>());

        // Get all unique dates from both files
        Set<String> allDates = new HashSet<>();
        dateMapA.values().forEach(d -> allDates.add(toYMD(d)));
        dateMapB.values().forEach(d -> allDates.add(toYMD(d)));
        List<String> sortedDates = new ArrayList<>(allDates);
        Collections.sort(sortedDates);

        summary.setTotalDates(sortedDates.size());
        summary.setTotalEmployees(matchingResult.getMatched().size());
        summary.setMatches(0);
        summary.setDisparities(0);

        List<ComparisonSummaryDTO.EmployeeComparisonDTO> employeeResults = new ArrayList<>();
        Map<String, List<ComparisonSummaryDTO.UnknownCodeDTO>> unknownCodes = new HashMap<>();
        Map<String, Integer> employeesWithMostDisparities = new HashMap<>();

        // Create rule map for quick lookup
        Map<String, RuleSetItem> ruleMap = new HashMap<>();
        for (RuleSetItem item : ruleSet.getItems()) {
            ruleMap.put(item.getCode(), item);
            System.out.println("Loaded rule: " + item.getCode() + " = " + item.getExpectedHours() + " hours");
        }

        // Process each matched employee
        for (MatchedEmployee match : matchingResult.getMatched()) {
            ComparisonSummaryDTO.EmployeeComparisonDTO empResult = new ComparisonSummaryDTO.EmployeeComparisonDTO();
            empResult.setName(match.getEmployeeA().getName());
            empResult.setMatchType(match.getMatchType());

            Map<String, ComparisonSummaryDTO.DateComparisonDTO> dateResults = new HashMap<>();

            ComparisonSummaryDTO.EmployeeSummary empSummary = new ComparisonSummaryDTO.EmployeeSummary();
            empSummary.setTotalDays(sortedDates.size());
            empSummary.setDaysWithData(0);
            empSummary.setMatches(0);
            empSummary.setDisparities(0);
            empSummary.setTotalHoursDiff(0.0);

            // Check each date
            for (String dateStr : sortedDates) {
                AttendanceData dataA = match.getEmployeeA().getAttendance().get(dateStr);
                AttendanceData dataB = match.getEmployeeB().getAttendance().get(dateStr);

                ComparisonSummaryDTO.DateComparisonDTO dateResult = new ComparisonSummaryDTO.DateComparisonDTO();
                dateResult.setDate(dateStr);

                if (dataA != null && "hours".equals(dataA.getType())) {
                    dateResult.setHoursValue((Double) dataA.getValue());
                    dateResult.setHoursOriginal(dataA.getOriginal());
                }

                if (dataB != null && "code".equals(dataB.getType())) {
                    dateResult.setCodeValue((String) dataB.getValue());
                    dateResult.setCodeOriginal(dataB.getOriginal());
                }

                // Case 1: Both files have data for this date
                if (dataA != null && dataB != null) {
                    empSummary.setDaysWithData(empSummary.getDaysWithData() + 1);

                    String code = ((String) dataB.getValue()).toUpperCase().trim();
                    RuleSetItem rule = ruleMap.get(code);

                    if (rule != null) {
                        dateResult.setExpectedHours(rule.getExpectedHours().intValue());

                        if ("hours".equals(dataA.getType())) {
                            Double hours = (Double) dataA.getValue();
                            double expectedHours = rule.getExpectedHours();

                            if (Math.abs(hours - expectedHours) < 0.01) {
                                dateResult.setDisparityType("match");
                                empSummary.setMatches(empSummary.getMatches() + 1);
                                summary.setMatches(summary.getMatches() + 1);
                                System.out.println("✓ Match: " + match.getEmployeeA().getName() + " on " + dateStr +
                                        " - Code: " + code + ", Hours: " + hours + ", Expected: " + expectedHours);
                            } else {
                                dateResult.setDisparityType("hoursMismatch");
                                Map<String, Object> details = new HashMap<>();
                                details.put("actual", hours);
                                details.put("expected", (int) expectedHours);
                                details.put("diff", hours - expectedHours);
                                dateResult.setDisparityDetails(details);

                                empSummary.setDisparities(empSummary.getDisparities() + 1);
                                empSummary.setTotalHoursDiff(empSummary.getTotalHoursDiff() + (hours - expectedHours));

                                summary.setDisparities(summary.getDisparities() + 1);
                                breakdown.setHoursMismatch(breakdown.getHoursMismatch() + 1);

                                System.out.println("✗ MISMATCH: " + match.getEmployeeA().getName() + " on " + dateStr +
                                        " - Code: " + code + ", Hours: " + hours + ", Expected: " + expectedHours +
                                        " (Diff: " + (hours - expectedHours) + ")");
                            }
                        } else {
                            dateResult.setDisparityType("noHours");
                            empSummary.setDisparities(empSummary.getDisparities() + 1);
                            summary.setDisparities(summary.getDisparities() + 1);
                            breakdown.setNoHours(breakdown.getNoHours() + 1);
                        }
                    } else {
                        // Unknown code
                        dateResult.setDisparityType("unknownCode");
                        Map<String, Object> details = new HashMap<>();
                        details.put("code", code);
                        dateResult.setDisparityDetails(details);

                        empSummary.setDisparities(empSummary.getDisparities() + 1);
                        summary.setDisparities(summary.getDisparities() + 1);
                        breakdown.setUnknownCode(breakdown.getUnknownCode() + 1);

                        unknownCodes.computeIfAbsent(code, k -> new ArrayList<>()).add(
                                createUnknownCodeEntry(match.getEmployeeA().getName(), dateStr, dataB.getOriginal()));

                        System.out.println("? Unknown Code: " + match.getEmployeeA().getName() + " on " + dateStr + " - Code: " + code);
                    }

                }
                // Case 2: Only hours file has data
                else if (dataA != null && dataB == null) {
                    empSummary.setDaysWithData(empSummary.getDaysWithData() + 1);
                    dateResult.setDisparityType("noCode");

                    Map<String, Object> details = new HashMap<>();
                    details.put("hours", dataA.getValue());
                    dateResult.setDisparityDetails(details);

                    empSummary.setDisparities(empSummary.getDisparities() + 1);
                    if ("hours".equals(dataA.getType())) {
                        empSummary.setTotalHoursDiff(empSummary.getTotalHoursDiff() + (Double) dataA.getValue());
                    }

                    summary.setDisparities(summary.getDisparities() + 1);
                    breakdown.setNoCode(breakdown.getNoCode() + 1);

                    System.out.println("! No Code: " + match.getEmployeeA().getName() + " on " + dateStr +
                            " - Hours: " + dataA.getValue() + " but no code");

                }
                // Case 3: Only codes file has data
                else if (dataA == null && dataB != null) {
                    empSummary.setDaysWithData(empSummary.getDaysWithData() + 1);
                    dateResult.setDisparityType("noHours");

                    Map<String, Object> details = new HashMap<>();
                    details.put("code", dataB.getValue());
                    dateResult.setDisparityDetails(details);

                    empSummary.setDisparities(empSummary.getDisparities() + 1);
                    summary.setDisparities(summary.getDisparities() + 1);
                    breakdown.setNoHours(breakdown.getNoHours() + 1);

                    System.out.println("! No Hours: " + match.getEmployeeA().getName() + " on " + dateStr +
                            " - Code: " + dataB.getValue() + " but no hours");
                }

                dateResults.put(dateStr, dateResult);
            }

            empResult.setDates(dateResults);
            empResult.setSummary(empSummary);
            employeeResults.add(empResult);

            if (empSummary.getDisparities() > 0) {
                employeesWithMostDisparities.put(empResult.getName(), empSummary.getDisparities());
            }
        }

        // Sort employees by disparities (descending)
        employeeResults.sort((a, b) ->
                b.getSummary().getDisparities().compareTo(a.getSummary().getDisparities()));

        result.setSummary(summary);
        result.setEmployees(employeeResults);
        result.setUnmatchedA(convertToUnmatchedDTO(matchingResult.getUnmatchedA()));
        result.setUnmatchedB(convertToUnmatchedDTO(matchingResult.getUnmatchedB()));
        result.setUnknownCodes(unknownCodes);
        result.setEmployeesWithMostDisparities(employeesWithMostDisparities);

        System.out.println("\n========== COMPARISON SUMMARY ==========");
        System.out.println("Total Matches: " + summary.getMatches());
        System.out.println("Total Disparities: " + summary.getDisparities());
        System.out.println("  - Hours Mismatch: " + breakdown.getHoursMismatch());
        System.out.println("  - No Code: " + breakdown.getNoCode());
        System.out.println("  - No Hours: " + breakdown.getNoHours());
        System.out.println("  - Unknown Code: " + breakdown.getUnknownCode());
        System.out.println("==========================================");

        return result;
    }

    private ComparisonSummaryDTO.UnknownCodeDTO createUnknownCodeEntry(String employee, String date, Object value) {
        ComparisonSummaryDTO.UnknownCodeDTO dto = new ComparisonSummaryDTO.UnknownCodeDTO();
        dto.setEmployee(employee);
        dto.setDate(date);
        dto.setValue(value);
        return dto;
    }

    private List<ComparisonSummaryDTO.UnmatchedEmployeeDTO> convertToUnmatchedDTO(List<EmployeeData> employees) {
        return employees.stream()
                .map(emp -> {
                    ComparisonSummaryDTO.UnmatchedEmployeeDTO dto =
                            new ComparisonSummaryDTO.UnmatchedEmployeeDTO();
                    dto.setName(emp.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void saveComparisonToDatabase(
            MultipartFile fileA,
            MultipartFile fileB,
            String sheetA,
            String sheetB,
            RuleSet ruleSet,
            ComparisonSummaryDTO result,
            String username) {

        try {
            Comparison comparison = new Comparison();
            comparison.setFile1Name(fileA.getOriginalFilename());
            comparison.setFile2Name(fileB.getOriginalFilename());
            comparison.setFile1Sheet(sheetA);
            comparison.setFile2Sheet(sheetB);
            comparison.setRuleSet(ruleSet);
            comparison.setCreatedBy(username);
            comparison.setSummaryStats(objectMapper.writeValueAsString(result.getSummary()));

            comparisonRepository.save(comparison);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public List<ComparisonSummaryDTO> getComparisonHistory(String username) {
        List<Comparison> comparisons = comparisonRepository.findByCreatedByOrderByCreatedAtDesc(username);
        List<ComparisonSummaryDTO> result = new ArrayList<>();

        for (Comparison comp : comparisons) {
            ComparisonSummaryDTO dto = new ComparisonSummaryDTO();
            try {
                if (comp.getSummaryStats() != null) {
                    ComparisonSummaryDTO.Summary summary =
                            objectMapper.readValue(comp.getSummaryStats(),
                                    ComparisonSummaryDTO.Summary.class);
                    dto.setSummary(summary);
                }
                result.add(dto);
            } catch (Exception e) {
                // Skip invalid entries
            }
        }

        return result;
    }

    public ComparisonSummaryDTO getComparisonById(Long id) {
        Comparison comparison = comparisonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comparison not found"));

        ComparisonSummaryDTO dto = new ComparisonSummaryDTO();
        try {
            if (comparison.getSummaryStats() != null) {
                ComparisonSummaryDTO.Summary summary =
                        objectMapper.readValue(comparison.getSummaryStats(),
                                ComparisonSummaryDTO.Summary.class);
                dto.setSummary(summary);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing comparison data");
        }

        return dto;
    }

    // Inner classes
    public static class ParsedFileData {
        private List<EmployeeData> employees;
        private Map<Integer, Date> dateColumnMap;

        public List<EmployeeData> getEmployees() { return employees; }
        public void setEmployees(List<EmployeeData> employees) { this.employees = employees; }
        public Map<Integer, Date> getDateColumnMap() { return dateColumnMap; }
        public void setDateColumnMap(Map<Integer, Date> dateColumnMap) { this.dateColumnMap = dateColumnMap; }
    }

    public static class EmployeeData {
        private String name;
        private int rowNumber;
        private Map<String, AttendanceData> attendance = new HashMap<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
        public Map<String, AttendanceData> getAttendance() { return attendance; }
        public void setAttendance(Map<String, AttendanceData> attendance) { this.attendance = attendance; }
    }

    public static class AttendanceData {
        private String type;
        private Object value;
        private Object original;
        private String date;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        public Object getOriginal() { return original; }
        public void setOriginal(Object original) { this.original = original; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }

    public static class MatchedEmployee {
        private EmployeeData employeeA;
        private EmployeeData employeeB;
        private String matchType;

        public MatchedEmployee(EmployeeData a, EmployeeData b, String type) {
            this.employeeA = a;
            this.employeeB = b;
            this.matchType = type;
        }

        public EmployeeData getEmployeeA() { return employeeA; }
        public void setEmployeeA(EmployeeData employeeA) { this.employeeA = employeeA; }
        public EmployeeData getEmployeeB() { return employeeB; }
        public void setEmployeeB(EmployeeData employeeB) { this.employeeB = employeeB; }
        public String getMatchType() { return matchType; }
        public void setMatchType(String matchType) { this.matchType = matchType; }
    }

    public static class EmployeeMatchingResult {
        private List<MatchedEmployee> matched;
        private List<EmployeeData> unmatchedA;
        private List<EmployeeData> unmatchedB;

        public List<MatchedEmployee> getMatched() { return matched; }
        public void setMatched(List<MatchedEmployee> matched) { this.matched = matched; }
        public List<EmployeeData> getUnmatchedA() { return unmatchedA; }
        public void setUnmatchedA(List<EmployeeData> unmatchedA) { this.unmatchedA = unmatchedA; }
        public List<EmployeeData> getUnmatchedB() { return unmatchedB; }
        public void setUnmatchedB(List<EmployeeData> unmatchedB) { this.unmatchedB = unmatchedB; }
    }
}
package com.eacattendance.component;

import com.eacattendance.dto.BiometricRecordDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class BiometricExcelParser {

    public List<BiometricRecordDTO> parseExcel(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        List<BiometricRecordDTO> records = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header

            BiometricRecordDTO record = new BiometricRecordDTO();
            record.setAction(getStringValue(row.getCell(0)));
            record.setAuthenticationMethod(getStringValue(row.getCell(1)));
            record.setEmployeeName(getStringValue(row.getCell(2)));
            record.setEmployeeId((long) row.getCell(3).getNumericCellValue());

            // Parse timestamp (format: "2025/05/01-11:57:37")
            String timestampStr = getStringValue(row.getCell(4));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
            record.setTimestamp(LocalDateTime.parse(timestampStr, formatter));

            records.add(record);
        }

        return records;
    }

    private String getStringValue(Cell cell) {
        return cell == null ? null : cell.getStringCellValue();
    }
}
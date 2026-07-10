package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeNameMatchingService {

    private final EmployeeRepository employeeRepository;

    public EmployeeNameMatchingService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Find employee by name from biometric device with flexible matching
     */
    public Optional<Employee> findEmployeeByDeviceName(String deviceName) {
        if (deviceName == null || deviceName.trim().isEmpty()) {
            return Optional.empty();
        }

        // Clean the name from device
        String cleanName = deviceName.trim().replaceAll("\\s+", " ");

        // Try to split into first and last name
        String[] nameParts = cleanName.split(" ");

        // Strategy 1: Try exact full name match
        Optional<Employee> exactMatch = employeeRepository.findByFullName(cleanName);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        // Strategy 2: If we have first and last name, try exact match on both
        if (nameParts.length >= 2) {
            String firstName = nameParts[0];
            String lastName = nameParts[nameParts.length - 1];

            Optional<Employee> firstLastMatch =
                    employeeRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName, lastName);
            if (firstLastMatch.isPresent()) {
                return firstLastMatch;
            }
        }

        // Strategy 3: Try case-insensitive partial match
        List<Employee> matches = employeeRepository.findByNameContainingIgnoreCase(cleanName);

        if (matches.size() == 1) {
            // Single match found
            return Optional.of(matches.get(0));
        } else if (matches.size() > 1) {
            // Multiple matches - try to find the best one
            return findBestMatch(matches, cleanName);
        }

        return Optional.empty();
    }

    public List<String> findSimilarEmployeeNames(String deviceName) {
        List<Employee> allEmployees = employeeRepository.findAll();
        List<String> suggestions = new ArrayList<>();

        if (deviceName == null || deviceName.isEmpty()) {
            return suggestions;
        }

        String[] deviceNameParts = deviceName.toLowerCase().split(" ");

        for (Employee emp : allEmployees) {
            String dbFullName = (emp.getFirstName() + " " + emp.getLastName()).toLowerCase();
            String dbFirstName = emp.getFirstName().toLowerCase();
            String dbLastName = emp.getLastName().toLowerCase();

            // Check if any part matches
            for (String part : deviceNameParts) {
                if (part.length() < 2) continue; // Skip very short parts

                if (dbFullName.contains(part) ||
                        dbFirstName.contains(part) ||
                        dbLastName.contains(part) ||
                        part.contains(dbFirstName) ||
                        part.contains(dbLastName)) {
                    suggestions.add(emp.getFirstName() + " " + emp.getLastName());
                    break;
                }
            }

            // Limit to 5 suggestions
            if (suggestions.size() >= 5) break;
        }

        return suggestions;
    }

    /**
     * Find the best match when multiple employees match the search term
     */
    private Optional<Employee> findBestMatch(List<Employee> employees, String searchTerm) {
        String searchLower = searchTerm.toLowerCase();

        Employee bestMatch = null;
        int highestScore = -1;

        for (Employee emp : employees) {
            String fullName = (emp.getFirstName() + " " + emp.getLastName()).toLowerCase();

            // Calculate similarity score
            int score = 0;

            // Check if full name contains search term
            if (fullName.contains(searchLower)) {
                score += 10;
            }

            // Check if search term contains full name
            if (searchLower.contains(fullName)) {
                score += 5;
            }

            // Bonus for exact word matching
            String[] searchWords = searchLower.split(" ");
            for (String word : searchWords) {
                if (fullName.contains(word)) {
                    score += 2;
                }
            }

            if (score > highestScore) {
                highestScore = score;
                bestMatch = emp;
            }
        }

        return bestMatch != null ? Optional.of(bestMatch) : Optional.empty();
    }

    /**
     * Extract name from device payload
     */
    public String extractNameFromDevicePayload(Object userObject, String userJson) {
        // Try to extract from user object first
        if (userObject instanceof java.util.Map) {
            java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) userObject;
            Object name = userMap.get("name");
            if (name != null && !name.toString().trim().isEmpty()) {
                return name.toString().trim();
            }
        }

        return null;
    }
}
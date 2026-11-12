package com.example.eac_backend_project.files;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Minimal employee API projection for sharing; never returns payroll or HR fields. */
@RestController
@RequestMapping("/api/employee")
public class EmployeeFileSharingController {
    private final FileSuiteService files;

    public EmployeeFileSharingController(FileSuiteService files) {
        this.files = files;
    }

    @GetMapping("/file-sharing")
    public Object recipients(@RequestParam(defaultValue = "") String q,@RequestParam(defaultValue = "2000") int limit) {
        return files.sharingDirectory(q,limit);
    }
}

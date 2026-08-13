package com.example.eac_backend_project.Service.settings;

import com.example.eac_backend_project.entity.settings.Department;
import com.example.eac_backend_project.entity.settings.JobPosition;
import com.example.eac_backend_project.repository.settings.DepartmentRepository;
import com.example.eac_backend_project.repository.settings.JobPositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    public List<Department> getAllDepartments() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    public Department createDepartment(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new RuntimeException("Department already exists: " + department.getName());
        }
        return departmentRepository.save(department);
    }

    public Department updateDepartment(Long id, Department department) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        existing.setName(department.getName());
        existing.setDescription(department.getDescription());
        existing.setManager(department.getManager());
        return departmentRepository.save(existing);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        // Check if any job position uses this department
        List<JobPosition> positions = jobPositionRepository.findByDepartmentId(id);
        if (!positions.isEmpty()) {
            throw new RuntimeException("Cannot delete department. It is used by " + positions.size() + " job position(s).");
        }
        departmentRepository.deleteById(id);
    }
}
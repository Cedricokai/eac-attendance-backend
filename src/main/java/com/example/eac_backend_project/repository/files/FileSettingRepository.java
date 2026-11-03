package com.example.eac_backend_project.repository.files;

import com.example.eac_backend_project.entity.files.FileSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileSettingRepository extends JpaRepository<FileSetting, Long> {

    Optional<FileSetting> findFirstByOrderByIdAsc();
}

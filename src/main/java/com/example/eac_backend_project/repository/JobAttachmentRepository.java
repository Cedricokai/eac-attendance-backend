package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.JobAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobAttachmentRepository extends JpaRepository<JobAttachment, Long> {

    @Query("SELECT ja FROM JobAttachment ja WHERE ja.job.id = :jobId")
    List<JobAttachment> findByJobId(@Param("jobId") Long jobId);

    @Modifying
    @Transactional
    @Query("DELETE FROM JobAttachment ja WHERE ja.job.id = :jobId")
    void deleteByJobId(@Param("jobId") Long jobId);

    @Query("SELECT ja FROM JobAttachment ja WHERE ja.job.id IN :jobIds")
    List<JobAttachment> findByJobIdIn(@Param("jobIds") List<Long> jobIds);

    // <<<<<<<<<< NEW: filter by job and document date range
    @Query("SELECT ja FROM JobAttachment ja WHERE ja.job.id = :jobId AND ja.documentDate BETWEEN :start AND :end")
    List<JobAttachment> findByJobIdAndDocumentDateBetween(@Param("jobId") Long jobId,
                                                          @Param("start") LocalDate start,
                                                          @Param("end") LocalDate end);
}
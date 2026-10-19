package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.IndividualReportSchedule;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

public interface IndividualReportScheduleRepository extends JpaRepository<IndividualReportSchedule, Long> {
    @Modifying @Transactional
    @Query("update IndividualReportSchedule s set s.nextRun = :next, s.lastAttempt = :now, s.lastResult = 'Sending', s.version = s.version + 1 where s.id = 1 and s.enabled = true and s.version = :version and s.nextRun <= :now")
    int claim(@Param("version") Long version, @Param("now") Instant now, @Param("next") Instant next);

    @Modifying @Transactional
    @Query("update IndividualReportSchedule s set s.lastResult = :result, s.version = s.version + 1 where s.id = 1 and s.lastAttempt = :attempt")
    int recordResult(@Param("attempt") Instant attempt, @Param("result") String result);
}

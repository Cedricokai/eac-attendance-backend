package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.Attendance;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface IndividualReportAttendanceRepository extends Repository<Attendance, Long> {
    // Background delivery has no web session; load the job needed by the Excel renderer here.
    @Query("select a from Attendance a join fetch a.employee e left join fetch e.job where a.date between :start and :end order by e.employeeId, a.date")
    List<Attendance> findAllForReportBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

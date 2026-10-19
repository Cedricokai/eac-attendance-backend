package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.repository.LeaveRepository;
import com.example.eac_backend_project.Service.email.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;

class LeaveApprovalSynchronizationTest {
    final LeaveRepository repository=mock(LeaveRepository.class);
    final ApprovedLeaveAttendanceService sync=mock(ApprovedLeaveAttendanceService.class);
    final LeaveService service=new LeaveService();
    LeaveApprovalSynchronizationTest() {
        ReflectionTestUtils.setField(service,"leaveRepository",repository);
        ReflectionTestUtils.setField(service,"approvedLeaveAttendanceService",sync);
        ReflectionTestUtils.setField(service,"emailService",mock(EmailService.class));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    Leave request() {
        var leave=new Leave(); leave.setId(1L); leave.setStatus("Pending");
        leave.setSupervisorStatus("Pending"); leave.setPlannerStatus("Approved");
        leave.setIsDeducted(true);
        when(repository.findById(1L)).thenReturn(Optional.of(leave)); return leave;
    }
    @Test void finalHrApprovalSynchronizesApprovedAttendance() {
        var leave=request(); service.hrApprove(1L,"Approved");
        verify(sync).synchronize(argThat(l -> l == leave && "Approved".equals(l.getStatus())));
    }
    @Test void bulkApprovalAlsoSynchronizesApprovedAttendance() {
        var leave=request(); service.bulkApproveLeaves(List.of(1L),"Approved");
        verify(sync).synchronize(argThat(l -> l == leave && "Approved".equals(l.getStatus())));
    }
    @Test void repeatingFinalApprovalRepairsAttendanceWithoutDeductingAgain() {
        var leave=request(); leave.setStatus("Approved"); leave.setHrStatus("Approved");
        service.hrApprove(1L,"Approved"); verify(sync).synchronize(leave);
        verify(repository,never()).save(any());
    }
}

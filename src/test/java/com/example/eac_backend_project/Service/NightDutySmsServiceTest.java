package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.settings.*;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NightDutySmsServiceTest {
 final NightDutySmsSettingsRepository settings=mock(NightDutySmsSettingsRepository.class);
 final AttendanceRepository attendance=mock(AttendanceRepository.class);
 final ArkeselSmsGateway gateway=mock(ArkeselSmsGateway.class);
 final NightDutySmsSettings config=new NightDutySmsSettings();
 final NightDutySmsService service=new NightDutySmsService(settings,attendance,gateway);
 NightDutySmsServiceTest() {
  config.setRecipient("+233244000000"); when(settings.findById(1L)).thenReturn(Optional.of(config));
  when(settings.save(any())).thenAnswer(call -> call.getArgument(0));
  when(attendance.findByDateBetweenOrderByDateDesc(any(),any())).thenReturn(List.of());
 }
 @Test void savesNormalizedRecipientAndValidates() {
  service.save("0244000000",true,"22:00","EAC"); assertEquals("+233244000000",config.getRecipient());
  assertThrows(IllegalArgumentException.class,()->service.save("wrong",true,"22:00","EAC"));
  assertThrows(IllegalArgumentException.class,()->service.save("",true,"22:00","EAC"));
 }
 @Test void credentialsRequiredAndDisabledScheduleNeverSends() throws Exception {
  assertThrows(IllegalStateException.class,service::send); service.scheduled(); verify(gateway,never()).send(any(),any(),any());
 }
 @Test void acceptedMessageUsesSavedRecipientAndPreventsImmediateDuplicates() throws Exception {
  when(gateway.configured()).thenReturn(true);
  assertTrue(service.send().contains("Accepted"));
  verify(gateway).send(eq(config.getRecipient()),eq("EAC"),contains("No current open night attendance"));
  assertThrows(IllegalStateException.class,service::send);
 }
 @Test void failedSendRecordsUnknownStatus() throws Exception {
  when(gateway.configured()).thenReturn(true); doThrow(new RuntimeException("timeout")).when(gateway).send(any(),any(),any());
  assertThrows(IllegalStateException.class,service::send); assertTrue(config.getLastResult().contains("unknown"));
 }
 @Test void rosterExcludesCheckedOutAndOldRecords() {
  var now=LocalDateTime.now(ZoneId.of("Africa/Accra"));
  var employee=new Employee(); employee.setId(1L); employee.setFirstName("Security"); employee.setLastName("Officer"); employee.setPhone("0244000000");
  var open=new Attendance(); open.setEmployee(employee); open.setDate(now.minusHours(1).toLocalDate()); open.setCheckIn(now.minusHours(1).toLocalTime()); open.setStatus("Present"); open.setShift("Night");
  when(attendance.findByDateBetweenOrderByDateDesc(any(),any())).thenReturn(List.of(open));
  assertTrue(service.preview().contains("Security Officer: 0244000000"));
  open.setCheckOut(now.toLocalTime()); assertFalse(service.preview().contains("Security Officer"));
  open.setCheckOut(null); open.setDate(now.minusHours(20).toLocalDate()); open.setCheckIn(now.minusHours(20).toLocalTime());
  assertFalse(service.preview().contains("Security Officer")); assertTrue(service.preview().contains("old open record"));
 }
}


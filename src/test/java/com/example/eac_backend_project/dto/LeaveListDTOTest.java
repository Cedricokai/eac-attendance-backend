package com.example.eac_backend_project.dto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LeaveListDTOTest {
    @Test void keepsDisplayAndDownloadMetadataWithoutAttachmentBytesOrLinkedEntities() throws Exception {
        var dto = new LeaveListDTO(1L, null, null, null, null, "Pending", null, null, null, null, null, null, null, null, null, null, null, "document.pdf", null, 1024L, null, null, 42L, "E042", "Test", "Employee", "Operations", "person@example.com", 3L, "Staff", true, 20, 8);
        var json = new ObjectMapper().findAndRegisterModules().valueToTree(dto);
        assertEquals(42L, json.path("employee").path("id").asLong());
        assertEquals("E042", json.path("employee").path("employeeId").asText());
        assertEquals("person@example.com", json.path("employee").path("email").asText());
        assertTrue(json.path("employee").path("category").path("excludeWeekendsFromLeave").asBoolean());
        assertEquals("document.pdf", json.path("attachmentFileName").asText());
        assertEquals(1024L, json.path("attachmentFileSize").asLong());
        assertFalse(json.has("attachmentFileContent"));
        assertFalse(json.has("approvalToken"));
        assertFalse(json.has("attendance"));
        assertFalse(json.has("overtime"));
        assertFalse(json.has("employeeId"));
    }
}

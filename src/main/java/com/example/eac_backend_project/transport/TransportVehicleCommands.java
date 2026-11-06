package com.example.eac_backend_project.transport;
import com.example.eac_backend_project.dto.VehicleDTO;
import com.example.eac_backend_project.Service.transport.VehicleService;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.eac_backend_project.transport.TransportAccess.fail;

@Service @Transactional
public class TransportVehicleCommands {
 private final VehicleService vehicles;private final TransportCoreService core;private final TransportAccess access;private final ObjectMapper json;
 public TransportVehicleCommands(VehicleService vehicles,TransportCoreService core,TransportAccess access,ObjectMapper json){this.vehicles=vehicles;this.core=core;this.access=access;this.json=json;}
 public VehicleDTO patch(Long id,JsonNode data){access.require("TRANSPORT_MANAGE_VEHICLES");var vehicle=core.lockedVehicle(id);VehicleDTO dto=vehicles.getVehicleById(id);try{json.readerForUpdating(dto).readValue(data.traverse(json));}catch(java.io.IOException exception){throw fail(400,"Vehicle fields contain an invalid value");}dto.setId(id);if(data.has("driverId")&&data.get("driverId").isNull())dto.setAssignedDriver("");if(data.has("acquisitionDate")&&data.get("acquisitionDate").isNull())core.profile(vehicle).acquisitionDate=null;if(data.has("initialOdometer")&&data.get("initialOdometer").isNull())core.profile(vehicle).initialOdometer=null;return vehicles.updateVehicle(id,dto);}
}

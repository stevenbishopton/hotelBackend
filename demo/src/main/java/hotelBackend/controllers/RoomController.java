package hotelBackend.controllers;

import hotelBackend.dtos.RoomDTO;
import hotelBackend.entities.RoomType;
import hotelBackend.services.RoomAvailabilityService;
import jakarta.validation.Valid;
import hotelBackend.response.ErrorResponse;
import hotelBackend.response.SuccessResponse;
import hotelBackend.entities.RoomEntity;
import hotelBackend.services.RoomService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "${cors.allowed-origins}") // Use the property
public class RoomController {
    private final RoomService roomService;
    private final RoomAvailabilityService roomAvailabilityService;

    @Autowired
    public RoomController(RoomService roomService, RoomAvailabilityService roomAvailabilityService) {
        this.roomService = roomService;
        this.roomAvailabilityService = roomAvailabilityService;
    }

    @GetMapping
    public ResponseEntity<List<RoomDTO>> getAllRooms() {
        List<RoomDTO> rooms = roomService.getAllRooms().stream()
                .map(RoomDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable Long id) {
        RoomEntity room = roomService.getRoomByid(id);
        return ResponseEntity.ok(RoomDTO.fromEntity(room));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<RoomDTO>> filterRooms(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "PRICE_ASC") String sortBy) {
        List<RoomDTO> filteredRooms = roomService.filterRooms(startDate, endDate, roomType, minPrice, maxPrice, sortBy);
        return ResponseEntity.ok(filteredRooms);
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomDTO>> getAvailableRooms(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<RoomDTO> availableRooms = roomService.getAvailableRooms(startDate, endDate);
        return ResponseEntity.ok(availableRooms);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoom(@Valid @RequestBody RoomEntity room) {
        try {
            RoomEntity createdRoom = roomService.createRoom(room);
            return ResponseEntity.status(HttpStatus.CREATED).body(RoomDTO.fromEntity(createdRoom));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Creation failed", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomEntity roomDetails) {
        try {
            RoomEntity updatedRoom = roomService.updateRoom(id, roomDetails);
            return ResponseEntity.ok(RoomDTO.fromEntity(updatedRoom));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Update failed", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            roomService.removeRoom(id);
            return ResponseEntity.ok(new SuccessResponse("Room successfully deleted"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Deletion failed", e.getMessage()));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Validation failed", errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Access denied", "You don't have permission to perform this action"));
    }
}
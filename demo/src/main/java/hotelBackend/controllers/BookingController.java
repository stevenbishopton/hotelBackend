package hotelBackend.controllers;

import hotelBackend.dtos.BookingDTO;
import hotelBackend.dtos.BookingResponse;
import hotelBackend.dtos.PaymentResponse;
import hotelBackend.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {
    //dependency
    private final BookingService bookingService;


    //get all bookings(paginated)
    @GetMapping
    public ResponseEntity<Page<BookingResponse>> getAllBookings(Pageable pageable) {
        log.info("Fetching all bookings with pagination");
        return ResponseEntity.ok(bookingService.getAllBookings(pageable));
    }


    //get a particular booking
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        log.info("Fetching booking with id: {}", id);
        return ResponseEntity.ok(bookingService.getBooking(id));
    }


    //get all bookings of a particular client
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<BookingResponse>> getClientBookings(@PathVariable Long clientId) {
        log.info("Fetching bookings for client: {}", clientId);
        return ResponseEntity.ok(bookingService.getClientBookings(clientId));
    }


    //get all bookings for a particular room
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<BookingResponse>> getRoomBookings(@PathVariable Long roomId) {
        log.info("Fetching bookings for room: {}", roomId);
        return ResponseEntity.ok(bookingService.getRoomBookings(roomId));
    }


    //create a booking
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingDTO bookingDTO) {
        log.info("Creating booking for room: {} and client: {}",
                bookingDTO.getRoomId(), bookingDTO.getClientId());
        return ResponseEntity.ok(bookingService.createBooking(bookingDTO));
    }


    //delete a particular room
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        log.info("Cancelling booking with id: {}", id);
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }


    //check particular room availability
    @GetMapping("/check-availability/{roomId}")
    public ResponseEntity<Boolean> checkRoomAvailability(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Checking availability for room: {} between {} and {}",
                roomId, startDate, endDate);
        return ResponseEntity.ok(bookingService.isRoomAvailable(roomId, startDate, endDate));
    }


}
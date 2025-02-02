package hotelBackend.services;

import hotelBackend.dtos.*;
import hotelBackend.entities.*;
import hotelBackend.exceptions.BookingException;
import hotelBackend.repositories.BookingRepository;
import hotelBackend.repositories.ClientRepository;
import hotelBackend.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getClientBookings(Long clientId) {
        return bookingRepository.findByClientId(clientId).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getRoomBookings(Long roomId) {
        return bookingRepository.findByRoomId(roomId).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found"));
        return convertToResponse(booking);
    }

    private BookingException createDetailedAvailabilityError(Long roomId, LocalDate startDate, LocalDate endDate) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BookingException("Room not found"));

        if (room.isUnderMaintenance()) {
            return new BookingException("Room is under maintenance");
        }

        List<BookingEntity> conflicts = bookingRepository.findConflictingBookings(
                roomId, startDate, endDate);

        if (!conflicts.isEmpty()) {
            BookingEntity conflict = conflicts.get(0);
            log.warn("Room {} has conflicting booking: {} to {}",
                    roomId, conflict.getBookingStartDate(), conflict.getBookingEndDate());

            return new BookingException(String.format(
                    "Room is already booked from %s to %s",
                    conflict.getBookingStartDate(),
                    conflict.getBookingEndDate()));
        }

        return new BookingException("Room is not available for the selected dates");
    }



    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponse createBooking(BookingDTO bookingDTO) {
        // Use the same validation as PaystackService
        RoomEntity room = roomRepository.findAvailableRoomWithLock(
                        bookingDTO.getRoomId(),
                        bookingDTO.getStartDate(),
                        bookingDTO.getEndDate())
                .orElseThrow(() -> new BookingException("Client not found"));

        ClientEntity client = clientRepository.findById(bookingDTO.getClientId())
                .orElseThrow(() -> new BookingException("Client not found"));

        BookingEntity booking = new BookingEntity();
        booking.setRoom(room);
        booking.setClient(client);
        booking.setBookingStartDate(bookingDTO.getStartDate());
        booking.setBookingEndDate(bookingDTO.getEndDate());
        booking.setAmountPaid(calculateTotalAmount(room, bookingDTO.getStartDate(), bookingDTO.getEndDate()));

        return convertToResponse(bookingRepository.save(booking));
    }


    @Transactional
    public void cancelBooking(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found"));

        if (booking.getBookingStartDate().isBefore(LocalDate.now())) {
            throw new BookingException("Cannot cancel past bookings");
        }

        bookingRepository.delete(booking);
    }

    @Transactional(readOnly = true)
    public boolean isRoomAvailable(Long roomId, LocalDate startDate, LocalDate endDate) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BookingException("Room not found"));
        try {
            validateRoomAvailability(room, startDate, endDate);
            return true;
        } catch (BookingException e) {
            return false;
        }
    }

    private void validateBookingDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BookingException("Start date must be before end date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BookingException("Cannot book in the past");
        }
    }

    private void validateRoomAvailability(RoomEntity room, LocalDate startDate, LocalDate endDate) {
        List<BookingEntity> conflictingBookings = bookingRepository.findConflictingBookings(
                room.getId(), startDate, endDate);

        if (!conflictingBookings.isEmpty()) {
            BookingEntity conflict = conflictingBookings.get(0);
            log.warn("Room {} has conflicting booking: {} to {}",
                    room.getId(),
                    conflict.getBookingStartDate(),
                    conflict.getBookingEndDate());

            throw new BookingException(String.format(
                    "Room is already booked during the requested period. Conflicting booking: %s to %s",
                    conflict.getBookingStartDate(),
                    conflict.getBookingEndDate()));
        }
    }


    private BigDecimal calculateTotalAmount(RoomEntity room, LocalDate startDate, LocalDate endDate) {
        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    private BookingResponse convertToResponse(BookingEntity booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .roomNumber(booking.getRoom().getRoomNumber())
                .clientName(booking.getClient().getName())
                .startDate(booking.getBookingStartDate())
                .endDate(booking.getBookingEndDate())
                .amountPaid(booking.getAmountPaid())
                .createdAt(booking.getCreatedAt())
                .build();
    }


}
package hotelBackend.services;

import hotelBackend.dtos.RoomDTO;
import hotelBackend.entities.BookingEntity;
import hotelBackend.entities.RoomEntity;
import hotelBackend.repositories.BookingRepository;
import hotelBackend.repositories.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RoomAvailabilityService {
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public RoomAvailabilityService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }


    public void validateAvailability(Long roomId, LocalDate startDate, LocalDate endDate) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.isUnderMaintenance()) {
            throw new IllegalStateException("Room is under maintenance");
        }

        List<BookingEntity> conflicts = bookingRepository
                .findConflictingBookings(roomId, startDate, endDate);

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Room is not available for the selected dates");
        }
    }

    public boolean isRoomAvailable(Long roomId, LocalDate startDate, LocalDate endDate) {
        try {
            validateAvailability(roomId, startDate, endDate);
            return true;
        } catch (IllegalStateException | IllegalArgumentException e) {
            return false;
        }
    }

    public List<LocalDate> getNextAvailableDates(Long roomId) {
        LocalDate startDate = LocalDate.now();
        List<LocalDate> availableDates = new ArrayList<>();

        // Look for next 30 days of availability
        for (int i = 0; i < 30; i++) {
            LocalDate checkDate = startDate.plusDays(i);
            if (isRoomAvailable(roomId, checkDate, checkDate.plusDays(1))) {
                availableDates.add(checkDate);
            }
        }

        return availableDates;
    }

    public List<RoomDTO> findAvailableRooms(LocalDate startDate, LocalDate endDate) {
        return roomRepository.findAllAvailableRooms(startDate, endDate).stream()
                .filter(room -> !room.isUnderMaintenance())
                .map(RoomDTO::fromEntity)  // Convert to DTO
                .collect(Collectors.toList());
    }
}
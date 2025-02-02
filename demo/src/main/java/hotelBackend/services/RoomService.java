package hotelBackend.services;

import hotelBackend.dtos.RoomAvailabilityResponse;
import hotelBackend.dtos.RoomDTO;
import hotelBackend.entities.RoomEntity;
import hotelBackend.entities.RoomType;
import hotelBackend.repositories.BookingRepository;
import hotelBackend.repositories.ClientRepository;
import hotelBackend.repositories.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository, ClientRepository clientRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.clientRepository = clientRepository;
    }
    @Autowired
    private RoomAvailabilityService roomAvailabilityService;




    public List<RoomDTO> getAvailableRooms(LocalDate startDate, LocalDate endDate) {
        return roomAvailabilityService.findAvailableRooms(startDate, endDate);
    }
    // Get all rooms
    public List<RoomEntity> getAllRooms(){
        return roomRepository.findAll();
    }


    public RoomEntity getRoomByid(Long ID){
        return roomRepository.getRoomByid(ID);
    }

    // Create a room
    public RoomEntity createRoom(RoomEntity roomEntity) {
        // Validation: Check if room number already exists
        if (roomRepository.existsByRoomNumber(roomEntity.getRoomNumber())) {
            throw new IllegalArgumentException("Room number already exists: " + roomEntity.getRoomNumber());
        }

        // Save and return the room
        return roomRepository.save(roomEntity);
    }

    // Remove a room
    public void removeRoom(Long roomId) {
        // Check if the room exists before attempting to delete
        roomRepository.findById(roomId).ifPresentOrElse(
                room -> roomRepository.deleteById(roomId),
                () -> {
                    throw new EntityNotFoundException("Room not found with ID: " + roomId);
                }
        );
    }
    public List<RoomDTO> filterRooms(
            LocalDate startDate,
            LocalDate endDate,
            RoomType roomType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy
    ) {
        // If no dates provided, use today and tomorrow
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = endDate != null ? endDate : effectiveStartDate.plusDays(1);

        // Get available rooms
        List<RoomEntity> availableRooms = roomRepository.findAllAvailableRooms(
                effectiveStartDate,
                effectiveEndDate
        );

        // Apply additional filters
        Stream<RoomEntity> roomStream = availableRooms.stream();

        if (roomType != null) {
            roomStream = roomStream.filter(room -> room.getRoomType() == roomType);
        }

        if (minPrice != null) {
            roomStream = roomStream.filter(room -> room.getPricePerNight().compareTo(minPrice) >= 0);
        }

        if (maxPrice != null) {
            roomStream = roomStream.filter(room -> room.getPricePerNight().compareTo(maxPrice) <= 0);
        }

        // Apply sorting
        if ("PRICE_DESC".equals(sortBy)) {
            roomStream = roomStream.sorted((r1, r2) -> r2.getPricePerNight().compareTo(r1.getPricePerNight()));
        } else {
            roomStream = roomStream.sorted((r1, r2) -> r1.getPricePerNight().compareTo(r2.getPricePerNight()));
        }

        return roomStream
                .map(room -> {
                    RoomDTO dto = RoomDTO.fromEntity(room);

                    // Check if room is available for the dates
                    boolean isAvailable = roomAvailabilityService.isRoomAvailable(
                            room.getId(),
                            effectiveStartDate,
                            effectiveEndDate
                    );

                    dto.setAvailable(isAvailable);

                    // If room is not available, get next available dates
                    if (!isAvailable && !room.isUnderMaintenance()) {
                        List<LocalDate> nextAvailableDates = roomAvailabilityService.getNextAvailableDates(room.getId());
                        dto.setNextAvailableDates(nextAvailableDates);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }
    // Update a room
    public RoomEntity updateRoom(Long roomId, RoomEntity roomDetails) {
        // Fetch the existing room entity
        RoomEntity existingRoom = roomRepository.findById(roomId).orElseThrow(() ->
                new EntityNotFoundException("Room not found with ID: " + roomId));

        // Update the properties of the room
        existingRoom.setRoomType(roomDetails.getRoomType());
        existingRoom.setRoomNumber(roomDetails.getRoomNumber());
        existingRoom.setDescription(roomDetails.getDescription());
        existingRoom.setImageUrl(roomDetails.getImageUrl());
        existingRoom.setPricePerNight(roomDetails.getPricePerNight());

        // Save and return the updated room entity
        return roomRepository.save(existingRoom);
    }
}


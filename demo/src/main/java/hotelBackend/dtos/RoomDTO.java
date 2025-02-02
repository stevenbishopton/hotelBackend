package hotelBackend.dtos;

import hotelBackend.entities.RoomEntity;
import hotelBackend.entities.RoomType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private Long id;
    private RoomType roomType;
    private String roomNumber;
    private String description;
    private String imageUrl;
    private boolean underMaintenance;
    private BigDecimal pricePerNight;
    private boolean available;
    private List<LocalDate> nextAvailableDates;

    public static RoomDTO fromEntity(RoomEntity room) {
        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setRoomType(room.getRoomType());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setDescription(room.getDescription());
        dto.setImageUrl(room.getImageUrl());
        dto.setUnderMaintenance(room.isUnderMaintenance());
        dto.setPricePerNight(room.getPricePerNight());
        dto.setAvailable(true);
        dto.setNextAvailableDates(new ArrayList<>());
        return dto;
    }
}
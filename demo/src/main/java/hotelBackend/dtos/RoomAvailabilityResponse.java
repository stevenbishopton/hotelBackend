package hotelBackend.dtos;

import lombok.Data;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Data
@Getter
@Setter
@Builder
public class RoomAvailabilityResponse {
    private boolean available;
    private String message;
    private List<LocalDate> nextAvailableDates;
}
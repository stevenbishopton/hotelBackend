package hotelBackend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Getter
@Setter
public class BookingDTO {
    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;
}
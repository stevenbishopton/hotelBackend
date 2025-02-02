package hotelBackend.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
public class BookingResponse {
    private Long id;
    private String roomNumber;
    private String clientName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amountPaid;
    private LocalDateTime createdAt;
}
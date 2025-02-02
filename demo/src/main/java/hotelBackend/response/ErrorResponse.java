package hotelBackend.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private Object details;
    private LocalDate conflictStartDate;
    private LocalDate conflictEndDate;

    public ErrorResponse(String message, Object details){
        this.message = message;
        this.details = details;
    }


}
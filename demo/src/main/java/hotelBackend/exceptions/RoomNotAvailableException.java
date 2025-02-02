package hotelBackend.exceptions;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class RoomNotAvailableException extends RuntimeException {
  private final String reason;
  private final LocalDate conflictStartDate;
  private final LocalDate conflictEndDate;

  public RoomNotAvailableException(String message, String reason,
                                   LocalDate conflictStart, LocalDate conflictEnd) {
    super(message);
    this.reason = reason;
    this.conflictStartDate = conflictStart;
    this.conflictEndDate = conflictEnd;
  }
}
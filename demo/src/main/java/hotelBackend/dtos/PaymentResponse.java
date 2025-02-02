package hotelBackend.dtos;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PaymentResponse {
    private String authorizationUrl;
    private String reference;
    private String message;
    private boolean status;


    public PaymentResponse(String authorizationUrl, String reference, String message, boolean status) {
        this.authorizationUrl = authorizationUrl;
        this.reference = reference;
        this.message = message;
        this.status = status;
    }
}
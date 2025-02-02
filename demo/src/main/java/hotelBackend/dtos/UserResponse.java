package hotelBackend.dtos;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;
@Getter
@Setter
@Data
public class UserResponse {
    private String username;
    private boolean enabled;

    public UserResponse(UserDetails userDetails) {
        this.username = userDetails.getUsername();
        this.enabled = userDetails.isEnabled();
    }
}
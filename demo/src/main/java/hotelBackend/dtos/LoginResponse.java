package hotelBackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@AllArgsConstructor
@Getter
@Setter
public class LoginResponse {
    private String token;
    private String username;
    private List<String> roles;
}
package hotelBackend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hotelBackend.dtos.PaymentInitiateRequest;
import hotelBackend.dtos.PaymentResponse;
import hotelBackend.entities.*;
import hotelBackend.exceptions.PaymentProcessingException;
import hotelBackend.repositories.BookingRepository;
import hotelBackend.repositories.ClientRepository;
import hotelBackend.repositories.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class PaystackService {

    @Value("${paystack.secretKey}")
    private String paystackSecretKey;

    @Value("${paystack.baseUrl}")
    private String paystackBaseUrl;

    @Value("${app.payment.callback-url}")
    private String callbackUrl;

    @Value("${app.payment.currency}")
    private String currency;

    @Value("${app.payment.success-url}")
    private String successUrl;

    @Value("${app.payment.cancel-url}")
    private String cancelUrl;

    private final RoomService roomService;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PaystackService(RoomService roomService,
                           ClientRepository clientRepository,
                           RoomRepository roomRepository,
                           BookingRepository bookingRepository,
                           RestTemplate restTemplate,
                           ObjectMapper objectMapper) {
        this.roomService = roomService;
        this.clientRepository = clientRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentResponse initiatePayment(PaymentInitiateRequest request) {
        RoomEntity room = roomRepository.findAvailableRoomWithLock(
                        request.getRoomId(),
                        request.getStartDate(),
                        request.getEndDate())
                .orElseThrow(() -> createDetailedAvailabilityError(
                        request.getRoomId(),
                        request.getStartDate(),
                        request.getEndDate()));

        try {
            BigDecimal totalAmount = calculateTotalAmount(request, room);
            ClientEntity client = processClient(request);
            return createPaystackPayment(room, client, request, totalAmount);
        } catch (Exception e) {
            log.error("Payment creation failed", e);
            throw new PaymentProcessingException("Unable to process payment: " + e.getMessage());
        }
    }

    @Transactional
    public boolean verifyPayment(String reference) {
        try {
            HttpHeaders headers = createPaystackHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    paystackBaseUrl + "/transaction/verify/" + reference,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            if (response.getBody() != null && response.getBody().get("status").asBoolean()) {
                JsonNode data = response.getBody().get("data");
                if ("success".equals(data.get("status").asText())) {
                    createBookingFromPayment(data);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Payment verification failed", e);
            throw new PaymentProcessingException("Failed to verify payment: " + e.getMessage());
        }
    }

    private void createBookingFromPayment(JsonNode data) {
        try {
            JsonNode metadata = data.get("metadata");
            Long roomId = Long.parseLong(metadata.get("roomId").asText());
            Long clientId = Long.parseLong(metadata.get("clientId").asText());

            RoomEntity room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new PaymentProcessingException("Room not found"));

            ClientEntity client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new PaymentProcessingException("Client not found"));

            // Check if booking already exists for this payment
            if (bookingRepository.findByPaymentReference(data.get("reference").asText()).isPresent()) {
                log.info("Booking already exists for payment reference: {}", data.get("reference").asText());
                return;
            }

            BookingEntity booking = new BookingEntity();
            booking.setRoom(room);
            booking.setClient(client);
            booking.setBookingStartDate(LocalDate.parse(metadata.get("startDate").asText()));
            booking.setBookingEndDate(LocalDate.parse(metadata.get("endDate").asText()));
            booking.setAmountPaid(new BigDecimal(metadata.get("totalAmount").asText()));
            booking.setPaymentReference(data.get("reference").asText());
            booking.setPaymentStatus("COMPLETED");

            BookingEntity savedBooking = bookingRepository.save(booking);
            client.getBookings().add(savedBooking);
            clientRepository.save(client);

            log.info("Successfully created booking {} for room {} and client {}",
                    savedBooking.getId(), roomId, clientId);
        } catch (Exception e) {
            log.error("Failed to create booking from payment", e);
            throw new PaymentProcessingException("Failed to create booking: " + e.getMessage());
        }
    }

    private PaymentProcessingException createDetailedAvailabilityError(
            Long roomId, LocalDate startDate, LocalDate endDate) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new PaymentProcessingException("Room not found"));

        if (room.isUnderMaintenance()) {
            return new PaymentProcessingException("Room is under maintenance");
        }

        List<BookingEntity> conflicts = bookingRepository
                .findConflictingBookings(roomId, startDate, endDate);

        if (!conflicts.isEmpty()) {
            BookingEntity conflict = conflicts.get(0);
            return new PaymentProcessingException(String.format(
                    "Room is already booked from %s to %s",
                    conflict.getBookingStartDate(),
                    conflict.getBookingEndDate()));
        }

        return new PaymentProcessingException("Room is not available");
    }

    private PaymentResponse createPaystackPayment(
            RoomEntity room,
            ClientEntity client,
            PaymentInitiateRequest request,
            BigDecimal totalAmount) {

        try {
            HttpHeaders headers = createPaystackHeaders();
            Map<String, Object> paymentRequest = createPaymentRequest(room, client, request, totalAmount);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentRequest, headers);

            String paystackInitializeUrl = paystackBaseUrl + "/transaction/initialize";
            ResponseEntity<String> response = restTemplate.postForEntity(
                    paystackInitializeUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode data = jsonResponse.get("data");

                log.info("Successfully created payment URL for room {} from {} to {}",
                        room.getId(), request.getStartDate(), request.getEndDate());

                return new PaymentResponse(
                        data.get("authorization_url").asText(),
                        data.get("reference").asText(),
                        "Authorization URL created",
                        true
                );
            }

            throw new PaymentProcessingException("Failed to get authorization URL from Paystack");
        } catch (Exception e) {
            log.error("Payment creation failed", e);
            throw new PaymentProcessingException("Failed to create payment: " + e.getMessage());
        }
    }

    private BigDecimal calculateTotalAmount(PaymentInitiateRequest request, RoomEntity room) {
        long nights = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (nights <= 0) {
            throw new IllegalArgumentException("Invalid booking duration");
        }
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    private ClientEntity processClient(PaymentInitiateRequest request) {
        try {
            Optional<ClientEntity> existingClient = clientRepository.findByPhoneNumber(request.getPhoneNumber());

            if (existingClient.isPresent()) {
                ClientEntity client = existingClient.get();
                client.setName(request.getName());
                client.setEmail(request.getEmail());
                return clientRepository.save(client);
            } else {
                ClientEntity newClient = new ClientEntity();
                newClient.setPhoneNumber(request.getPhoneNumber());
                newClient.setName(request.getName());
                newClient.setEmail(request.getEmail());
                newClient.setBookings(new ArrayList<>());
                return clientRepository.save(newClient);
            }
        } catch (Exception e) {
            throw new PaymentProcessingException("Failed to process client information", e);
        }
    }

    private HttpHeaders createPaystackHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + paystackSecretKey);
        return headers;
    }

    private Map<String, Object> createPaymentRequest(RoomEntity room, ClientEntity client,
                                                     PaymentInitiateRequest request, BigDecimal totalAmount) {
        Map<String, Object> paymentRequest = new HashMap<>();
        String reference = generateReference(room.getId(), client.getId());

        paymentRequest.put("email", request.getEmail());
        paymentRequest.put("amount", totalAmount.multiply(new BigDecimal("100")).intValue());
        paymentRequest.put("currency", currency);
        paymentRequest.put("reference", reference);

        // Add redirect URLs with reference
        paymentRequest.put("callback_url", successUrl + "?reference=" + reference);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("roomId", room.getId().toString());
        metadata.put("clientId", client.getId().toString());
        metadata.put("startDate", request.getStartDate().toString());
        metadata.put("endDate", request.getEndDate().toString());
        metadata.put("totalAmount", totalAmount.toString());
        metadata.put("clientName", request.getName());
        metadata.put("phoneNumber", request.getPhoneNumber());
        metadata.put("success_url", successUrl);
        metadata.put("cancel_url", cancelUrl);

        paymentRequest.put("metadata", metadata);

        log.info("Created payment request: {}", paymentRequest);
        return paymentRequest;
    }

    private String generateReference(Long roomId, Long clientId) {
        return String.format("HOTEL-%d-%d-%d", roomId, clientId, System.currentTimeMillis());
    }
}
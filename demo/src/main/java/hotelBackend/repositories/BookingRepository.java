package hotelBackend.repositories;

import hotelBackend.entities.BookingEntity;
import hotelBackend.entities.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    List<BookingEntity> findByClientId(Long clientId);

    List<BookingEntity> findByRoomId(Long roomId);

    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.room.id = :roomId " +
            "AND ((:startDate BETWEEN b.bookingStartDate AND b.bookingEndDate) " +
            "OR (:endDate BETWEEN b.bookingStartDate AND b.bookingEndDate) " +
            "OR (b.bookingStartDate BETWEEN :startDate AND :endDate))")
    List<BookingEntity> findConflictingBookings(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    Optional<BookingEntity> findByPaymentReference(String paymentReference);
    }






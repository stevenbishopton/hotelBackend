package hotelBackend.repositories;

import hotelBackend.entities.RoomEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
    RoomEntity findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    RoomEntity getRoomByid(Long id);

    @Query("SELECT r FROM RoomEntity r " +
            "WHERE r.id = :roomId " +
            "AND NOT r.underMaintenance " +
            "AND NOT EXISTS (" +
            "    SELECT b FROM BookingEntity b " +
            "    WHERE b.room.id = r.id " +
            "    AND ((:startDate BETWEEN b.bookingStartDate AND b.bookingEndDate) " +
            "         OR (:endDate BETWEEN b.bookingStartDate AND b.bookingEndDate) " +
            "         OR (b.bookingStartDate BETWEEN :startDate AND :endDate)))")
    Optional<RoomEntity> findAvailableRoom(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("""
    SELECT r FROM RoomEntity r 
    WHERE r.id = :roomId
    AND r.underMaintenance = false
    AND NOT EXISTS (
        SELECT b FROM BookingEntity b
        WHERE b.room.id = r.id
        AND (
            (:startDate < b.bookingEndDate)
            AND (:endDate > b.bookingStartDate)
        )
    )
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomEntity> findAvailableRoomWithLock(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT r FROM RoomEntity r " +
            "WHERE NOT r.underMaintenance " +
            "AND r.id NOT IN (" +
            "    SELECT b.room.id FROM BookingEntity b " +
            "    WHERE b.bookingStartDate <= :endDate " +
            "    AND b.bookingEndDate >= :startDate)")
    List<RoomEntity> findAllAvailableRooms(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

package ru.practicum.shareit.booking;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByBookerIdOrderByStartDesc(Long bookerId);

    List<Booking> findAllByBookerId(Long bookerId);

    @Query("SELECT b FROM Booking b WHERE b.itemId IN (SELECT i.id FROM Item i WHERE i.ownerId = :ownerId) ORDER BY b.start DESC")
    List<Booking> findAllByItemOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT b FROM Booking b WHERE b.itemId = :itemId AND b.status = 'APPROVED' AND b.start < :now ORDER BY b.start DESC")
    List<Booking> findLastBookings(@Param("itemId") Long itemId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.itemId = :itemId AND b.status = 'APPROVED' AND b.start > :now ORDER BY b.start ASC")
    List<Booking> findNextBookings(@Param("itemId") Long itemId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.bookerId = :userId AND b.itemId = :itemId AND b.end < :now AND b.status = 'APPROVED'")
    boolean existsCompletedBooking(@Param("userId") Long userId, @Param("itemId") Long itemId, @Param("now") LocalDateTime now);
}
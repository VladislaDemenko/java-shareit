package ru.practicum.shareit.booking;

import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class BookingRepository {
    private final Map<Long, Booking> bookings = new HashMap<>();
    private long currentId = 1;

    public Booking save(Booking booking) {
        if (booking.getId() == null) {
            booking.setId(currentId++);
        }
        bookings.put(booking.getId(), booking);
        return booking;
    }

    public Optional<Booking> findById(Long id) {
        return Optional.ofNullable(bookings.get(id));
    }

    public List<Booking> findAllByBookerId(Long bookerId) {
        return bookings.values().stream()
                .filter(booking -> booking.getBookerId().equals(bookerId))
                .toList();
    }

    public List<Booking> findAllByItemOwnerId(Long ownerId) {
        // В реальном приложении нужно связывать с Item
        // Для текущего спринта - заглушка
        return new ArrayList<>();
    }
}
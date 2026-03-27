package ru.practicum.shareit.booking;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;

import java.util.*;
import java.util.stream.Collectors;

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
                .sorted(Comparator.comparing(Booking::getStart).reversed())
                .collect(Collectors.toList());
    }

    public List<Booking> findAllByItemOwnerId(Long ownerId, ItemRepository itemRepository) {
        // Получаем все вещи владельца
        List<Item> ownerItems = itemRepository.findAllByOwnerId(ownerId);
        Set<Long> itemIds = ownerItems.stream().map(Item::getId).collect(Collectors.toSet());

        return bookings.values().stream()
                .filter(booking -> itemIds.contains(booking.getItemId()))
                .sorted(Comparator.comparing(Booking::getStart).reversed())
                .collect(Collectors.toList());
    }
}
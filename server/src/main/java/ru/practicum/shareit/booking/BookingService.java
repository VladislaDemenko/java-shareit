package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;

import java.util.List;

public interface BookingService {
    BookingDto create(Long userId, BookingDto bookingDto);
    BookingDto approve(Long userId, Long bookingId, Boolean approved);
    BookingDto getById(Long userId, Long bookingId);
    List<BookingDto> getAllByUser(Long userId, String state, Integer from, Integer size);
    List<BookingDto> getAllByOwner(Long userId, String state, Integer from, Integer size);
    BookingShortDto getLastBooking(Long itemId);
    BookingShortDto getNextBooking(Long itemId);
    boolean hasUserBookedItem(Long userId, Long itemId);
}
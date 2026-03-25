package ru.practicum.shareit.booking;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.dto.BookingDto;

@Component
public class BookingMapper {

    public BookingDto toDto(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingDto(
                booking.getId(),
                booking.getStart(),
                booking.getEnd(),
                booking.getItemId(),
                booking.getBookerId(),
                booking.getStatus() != null ? booking.getStatus().name() : null
        );
    }

    public Booking toEntity(BookingDto bookingDto) {
        if (bookingDto == null) {
            return null;
        }
        Booking booking = new Booking();
        booking.setId(bookingDto.getId());
        booking.setStart(bookingDto.getStart());
        booking.setEnd(bookingDto.getEnd());
        booking.setItemId(bookingDto.getItemId());
        booking.setBookerId(bookingDto.getBookerId());
        if (bookingDto.getStatus() != null) {
            booking.setStatus(Booking.BookingStatus.valueOf(bookingDto.getStatus()));
        }
        return booking;
    }
}
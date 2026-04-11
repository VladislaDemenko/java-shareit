package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingDto bookingDto) {
        log.info("Creating booking for user: {}", userId);

        validateUser(userId);
        validateBookingDates(bookingDto);

        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Вещь с id=%d не найдена", bookingDto.getItemId())));

        if (item.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException(
                    String.format("Нельзя бронировать свою вещь с id=%d", item.getId()));
        }

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new IllegalArgumentException(
                    String.format("Вещь с id=%d недоступна для бронирования", item.getId()));
        }

        Booking booking = bookingMapper.toEntity(bookingDto);
        booking.setBookerId(userId);
        booking.setItemId(item.getId());
        booking.setStatus(Booking.BookingStatus.WAITING);
        booking = bookingRepository.save(booking);

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto approve(Long userId, Long bookingId, Boolean approved) {
        log.info("Approving booking {} with status: {}", bookingId, approved);

        validateUser(userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Бронирование с id=%d не найдено", bookingId)));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Вещь с id=%d не найдена", booking.getItemId())));

        if (!item.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException(
                    String.format("Подтвердить бронирование может только владелец вещи с id=%d", item.getId()));
        }

        if (booking.getStatus() != Booking.BookingStatus.WAITING) {
            throw new IllegalArgumentException(
                    String.format("Бронирование с id=%d уже подтверждено или отклонено", bookingId));
        }

        booking.setStatus(approved ? Booking.BookingStatus.APPROVED : Booking.BookingStatus.REJECTED);
        Booking savedBooking = bookingRepository.save(booking);

        return bookingMapper.toDto(savedBooking);
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        log.info("Getting booking {} for user {}", bookingId, userId);

        validateUser(userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Бронирование с id=%d не найдено", bookingId)));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Вещь с id=%d не найдена", booking.getItemId())));

        if (!booking.getBookerId().equals(userId) && !item.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException(
                    String.format("Просмотреть бронирование может только автор (id=%d) или владелец вещи (id=%d)",
                            booking.getBookerId(), item.getOwnerId()));
        }

        return bookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getAllByUser(Long userId, String state, Integer from, Integer size) {
        log.info("Getting all bookings for user {} with state {}", userId, state);

        validateUser(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        List<Booking> bookings = bookingRepository.findAllByBookerIdOrderByStartDesc(userId, pageable);
        List<Booking> filtered = filterByState(bookings, state);

        return filtered.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getAllByOwner(Long userId, String state, Integer from, Integer size) {
        log.info("Getting all bookings for owner {} with state {}", userId, state);

        validateUser(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Booking> bookings = bookingRepository.findAllByItemOwnerId(userId, pageable);
        List<Booking> filtered = filterByState(bookings, state);

        return filtered.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BookingShortDto getLastBooking(Long itemId) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 1);
        List<Booking> bookings = bookingRepository.findLastBookings(itemId, now, pageable);

        if (bookings.isEmpty()) {
            return null;
        }

        Booking booking = bookings.get(0);
        return new BookingShortDto(booking.getId(), booking.getBookerId());
    }

    @Override
    public BookingShortDto getNextBooking(Long itemId) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 1);
        List<Booking> bookings = bookingRepository.findNextBookings(itemId, now, pageable);

        if (bookings.isEmpty()) {
            return null;
        }

        Booking booking = bookings.get(0);
        return new BookingShortDto(booking.getId(), booking.getBookerId());
    }

    @Override
    public boolean hasUserBookedItem(Long userId, Long itemId) {
        return bookingRepository.existsCompletedBooking(userId, itemId, LocalDateTime.now());
    }

    private List<Booking> filterByState(List<Booking> bookings, String state) {
        LocalDateTime now = LocalDateTime.now();

        switch (state.toUpperCase()) {
            case "ALL":
                return bookings;
            case "CURRENT":
                return bookings.stream()
                        .filter(b -> b.getStart().isBefore(now) && b.getEnd().isAfter(now))
                        .collect(Collectors.toList());
            case "PAST":
                return bookings.stream()
                        .filter(b -> b.getEnd().isBefore(now))
                        .collect(Collectors.toList());
            case "FUTURE":
                return bookings.stream()
                        .filter(b -> b.getStart().isAfter(now))
                        .collect(Collectors.toList());
            case "WAITING":
                return bookings.stream()
                        .filter(b -> b.getStatus() == Booking.BookingStatus.WAITING)
                        .collect(Collectors.toList());
            case "REJECTED":
                return bookings.stream()
                        .filter(b -> b.getStatus() == Booking.BookingStatus.REJECTED)
                        .collect(Collectors.toList());
            default:
                throw new IllegalArgumentException("Unknown state: " + state);
        }
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException(
                    String.format("Пользователь с id=%d не найден", userId));
        }
    }

    private void validateBookingDates(BookingDto bookingDto) {
        if (bookingDto.getStart() == null || bookingDto.getEnd() == null) {
            throw new IllegalArgumentException("Даты начала и окончания не могут быть пустыми");
        }
        if (bookingDto.getEnd().isBefore(bookingDto.getStart()) || bookingDto.getEnd().equals(bookingDto.getStart())) {
            throw new IllegalArgumentException("Дата окончания должна быть позже даты начала");
        }
        if (bookingDto.getStart().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Дата начала не может быть в прошлом");
        }
    }
}
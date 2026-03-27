package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingMapper bookingMapper;

    @Override
    public BookingDto create(Long userId, BookingDto bookingDto) {
        log.info("Creating booking for user: {}", userId);

        validateUser(userId);

        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Вещь не найдена"));

        if (item.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Нельзя бронировать свою вещь");
        }

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new IllegalArgumentException("Вещь недоступна для бронирования");
        }

        Booking booking = bookingMapper.toEntity(bookingDto);
        booking.setBookerId(userId);
        booking.setItemId(item.getId());
        booking.setStatus(Booking.BookingStatus.WAITING);
        booking = bookingRepository.save(booking);

        return bookingMapper.toDto(booking);
    }

    @Override
    public BookingDto approve(Long userId, Long bookingId, Boolean approved) {
        log.info("Approving booking {} with status: {}", bookingId, approved);

        validateUser(userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено"));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Вещь не найдена"));

        if (!item.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Подтвердить бронирование может только владелец вещи");
        }

        if (booking.getStatus() != Booking.BookingStatus.WAITING) {
            throw new IllegalArgumentException("Бронирование уже подтверждено или отклонено");
        }

        booking.setStatus(approved ? Booking.BookingStatus.APPROVED : Booking.BookingStatus.REJECTED);
        bookingRepository.save(booking);

        return bookingMapper.toDto(booking);
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        log.info("Getting booking {} for user {}", bookingId, userId);

        validateUser(userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено"));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Вещь не найдена"));

        if (!booking.getBookerId().equals(userId) && !item.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Просмотреть бронирование может только автор или владелец вещи");
        }

        return bookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getAllByUser(Long userId, String state) {
        log.info("Getting all bookings for user {} with state {}", userId, state);

        validateUser(userId);

        List<Booking> bookings = bookingRepository.findAllByBookerId(userId);

        return filterByState(bookings, state).stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getAllByOwner(Long userId, String state) {
        log.info("Getting all bookings for owner {} with state {}", userId, state);

        validateUser(userId);

        List<Booking> bookings = bookingRepository.findAllByItemOwnerId(userId, itemRepository);

        return filterByState(bookings, state).stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
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
                return bookings;
        }
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
    }
}
package ru.practicum.shareit.booking.dto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.user.UserRepository;
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

        if (!itemRepository.existsById(bookingDto.getItemId())) {
            throw new IllegalArgumentException("Вещь не найдена");
        }

        Booking booking = bookingMapper.toEntity(bookingDto);
        booking.setBookerId(userId);
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

        // Проверка прав владельца вещи
        // В реальном приложении нужно проверить, что userId - владелец вещи
        // Для текущего спринта - заглушка

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

        return bookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getAllByUser(Long userId, String state) {
        log.info("Getting all bookings for user {} with state {}", userId, state);

        validateUser(userId);

        return bookingRepository.findAllByBookerId(userId).stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getAllByOwner(Long userId, String state) {
        log.info("Getting all bookings for owner {} with state {}", userId, state);

        validateUser(userId);

        return bookingRepository.findAllByItemOwnerId(userId).stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
    }
}
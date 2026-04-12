package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.comment.Comment;
import ru.practicum.shareit.comment.CommentDto;
import ru.practicum.shareit.comment.CommentRepository;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final ItemRequestRepository requestRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        log.info("Creating item for user: {}", userId);

        validateUser(userId);

        if (itemDto.getRequestId() != null) {
            requestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
        }

        Item item = itemMapper.toEntity(itemDto, userId);
        item = itemRepository.save(item);

        return itemMapper.toDto(item);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        log.info("Updating item {} for user {}", itemId, userId);

        validateUser(userId);

        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Вещь не найдена"));

        if (!existingItem.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Редактировать вещь может только владелец");
        }

        if (itemDto.getName() != null) {
            existingItem.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            existingItem.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }

        itemRepository.save(existingItem);

        return getById(userId, itemId);
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        log.info("Getting item by id: {} for user: {}", itemId, userId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Вещь не найдена"));

        ItemDto itemDto = itemMapper.toDto(item);

        if (userId != null && item.getOwnerId().equals(userId)) {
            itemDto.setLastBooking(bookingService.getLastBooking(itemId));
            itemDto.setNextBooking(bookingService.getNextBooking(itemId));
        }

        List<CommentDto> comments = commentRepository.findAllByItemIdOrderByCreatedDesc(itemId).stream()
                .map(this::toCommentDto)
                .collect(Collectors.toList());
        itemDto.setComments(comments);

        return itemDto;
    }

    @Override
    public List<ItemDto> getAllByOwner(Long userId, Integer from, Integer size) {
        log.info("Getting all items for user: {}", userId);

        validateUser(userId);

        Pageable pageable = PageRequest.of(from / size, size);

        List<Item> items = itemRepository.findAllByOwnerIdOrderByIdAsc(userId, pageable);
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());

        Map<Long, List<CommentDto>> commentsByItemId = commentRepository.findAllByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(Comment::getItemId,
                        Collectors.mapping(this::toCommentDto, Collectors.toList())));

        LocalDateTime now = LocalDateTime.now();
        Pageable one = PageRequest.of(0, 1);

        Map<Long, List<BookingShortDto>> lastBookings = bookingRepository.findLastBookingsForItems(itemIds, now).stream()
                .collect(Collectors.groupingBy(b -> b.getItemId(),
                        Collectors.mapping(b -> new BookingShortDto(b.getId(), b.getBookerId()), Collectors.toList())));

        Map<Long, List<BookingShortDto>> nextBookings = bookingRepository.findNextBookingsForItems(itemIds, now).stream()
                .collect(Collectors.groupingBy(b -> b.getItemId(),
                        Collectors.mapping(b -> new BookingShortDto(b.getId(), b.getBookerId()), Collectors.toList())));

        return items.stream()
                .map(item -> {
                    ItemDto dto = itemMapper.toDto(item);
                    dto.setComments(commentsByItemId.getOrDefault(item.getId(), new ArrayList<>()));
                    List<BookingShortDto> last = lastBookings.get(item.getId());
                    dto.setLastBooking(last != null && !last.isEmpty() ? last.get(0) : null);
                    List<BookingShortDto> next = nextBookings.get(item.getId());
                    dto.setNextBooking(next != null && !next.isEmpty() ? next.get(0) : null);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> search(String text, Integer from, Integer size) {
        log.info("Searching items with text: {}", text);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(from / size, size);

        return itemRepository.searchAvailable(text, pageable).stream()
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        log.info("Adding comment for user {} to item {}", userId, itemId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Вещь не найдена"));

        if (!bookingService.hasUserBookedItem(userId, itemId)) {
            throw new IllegalArgumentException("Пользователь не брал эту вещь в аренду");
        }

        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItemId(itemId);
        comment.setAuthorId(userId);
        comment.setCreated(LocalDateTime.now());

        comment = commentRepository.save(comment);

        CommentDto result = toCommentDto(comment);
        result.setAuthorName(user.getName());

        return result;
    }

    private CommentDto toCommentDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setCreated(comment.getCreated());

        userRepository.findById(comment.getAuthorId()).ifPresent(user ->
                dto.setAuthorName(user.getName())
        );

        return dto;
    }

    private void validateUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null");
        }
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
    }
}
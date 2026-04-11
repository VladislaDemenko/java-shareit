package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.comment.Comment;
import ru.practicum.shareit.comment.CommentRepository;
import ru.practicum.shareit.comment.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final BookingService bookingService;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        log.info("Creating item for user: {}", userId);

        validateUser(userId);

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
    public List<ItemDto> getAllByOwner(Long userId) {
        log.info("Getting all items for user: {}", userId);

        validateUser(userId);

        return itemRepository.findAllByOwnerIdOrderByIdAsc(userId).stream()
                .map(item -> {
                    ItemDto itemDto = itemMapper.toDto(item);
                    itemDto.setLastBooking(bookingService.getLastBooking(item.getId()));
                    itemDto.setNextBooking(bookingService.getNextBooking(item.getId()));

                    List<CommentDto> comments = commentRepository.findAllByItemIdOrderByCreatedDesc(item.getId()).stream()
                            .map(this::toCommentDto)
                            .collect(Collectors.toList());
                    itemDto.setComments(comments);

                    return itemDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> search(String text) {
        log.info("Searching items with text: {}", text);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.searchAvailable(text).stream()
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
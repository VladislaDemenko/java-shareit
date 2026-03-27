package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        log.info("Creating item for user: {}", userId);

        validateUser(userId);

        Item item = itemMapper.toEntity(itemDto, userId);
        item = itemRepository.save(item);

        ItemDto result = itemMapper.toDto(item);
        log.info("Created item: {}", result);

        return result;
    }

    @Override
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

        return itemMapper.toDto(existingItem);
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        log.info("Getting item by id: {} for user: {}", itemId, userId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Вещь не найдена"));

        return itemMapper.toDto(item);
    }

    @Override
    public List<ItemDto> getAllByOwner(Long userId) {
        log.info("Getting all items for user: {}", userId);

        validateUser(userId);

        return itemRepository.findAllByOwnerId(userId).stream()
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> search(String text) {
        log.info("Searching items with text: {}", text);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<Item> items = itemRepository.searchAvailable(text);
        log.info("Found {} items for search text: {}", items.size(), text);

        return items.stream()
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
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
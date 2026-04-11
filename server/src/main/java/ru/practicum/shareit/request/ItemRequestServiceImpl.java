package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemRequestMapper requestMapper;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestDto requestDto) {
        log.info("Creating request for user: {}", userId);

        validateUser(userId);

        ItemRequest request = requestMapper.toEntity(requestDto, userId);
        request = requestRepository.save(request);

        return requestMapper.toDto(request);
    }

    @Override
    public ItemRequestWithItemsDto getById(Long userId, Long requestId) {
        log.info("Getting request {} for user {}", requestId, userId);

        validateUser(userId);

        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));

        List<Item> items = itemRepository.findAllByRequestId(requestId);
        List<ItemDto> itemDtos = items.stream()
                .map(itemMapper::toDto)
                .collect(Collectors.toList());

        return toRequestWithItemsDto(request, itemDtos);
    }

    @Override
    public List<ItemRequestWithItemsDto> getAllByUser(Long userId) {
        log.info("Getting all requests for user: {}", userId);

        validateUser(userId);

        List<ItemRequest> requests = requestRepository.findAllByRequesterIdOrderByCreatedDesc(userId);

        return enrichWithItems(requests);
    }

    @Override
    public List<ItemRequestWithItemsDto> getAll(Long userId, Integer from, Integer size) {
        log.info("Getting all requests for user {} with pagination", userId);

        validateUser(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "created"));

        List<ItemRequest> requests = requestRepository.findAllByRequesterIdNot(userId, pageable);

        return enrichWithItems(requests);
    }

    private List<ItemRequestWithItemsDto> enrichWithItems(List<ItemRequest> requests) {
        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .collect(Collectors.toList());

        List<Item> items = itemRepository.findAllByRequestIdIn(requestIds);
        Map<Long, List<ItemDto>> itemsByRequestId = items.stream()
                .collect(Collectors.groupingBy(Item::getRequestId,
                        Collectors.mapping(itemMapper::toDto, Collectors.toList())));

        return requests.stream()
                .map(request -> toRequestWithItemsDto(request, itemsByRequestId.getOrDefault(request.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private ItemRequestWithItemsDto toRequestWithItemsDto(ItemRequest request, List<ItemDto> items) {
        return new ItemRequestWithItemsDto(
                request.getId(),
                request.getDescription(),
                request.getCreated(),
                items
        );
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
    }
}
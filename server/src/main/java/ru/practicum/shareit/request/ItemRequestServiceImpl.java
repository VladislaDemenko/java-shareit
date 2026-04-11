package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRequestMapper requestMapper;

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
    public ItemRequestDto getById(Long userId, Long requestId) {
        log.info("Getting request {} for user {}", requestId, userId);

        validateUser(userId);

        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));

        return requestMapper.toDto(request);
    }

    @Override
    public List<ItemRequestDto> getAllByUser(Long userId) {
        log.info("Getting all requests for user: {}", userId);

        validateUser(userId);

        return requestRepository.findAllByRequesterIdOrderByCreatedDesc(userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemRequestDto> getAll(Long userId, int from, int size) {
        log.info("Getting all requests for user {} with pagination", userId);

        validateUser(userId);

        Pageable pageable = PageRequest.of(from / size, size);

        return requestRepository.findAllExceptUser(userId, pageable).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
    }
}
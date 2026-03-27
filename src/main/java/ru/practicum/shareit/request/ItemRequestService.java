package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto create(Long userId, ItemRequestDto requestDto);
    ItemRequestDto getById(Long userId, Long requestId);
    List<ItemRequestDto> getAllByUser(Long userId);
    List<ItemRequestDto> getAll(Long userId, int from, int size);
}
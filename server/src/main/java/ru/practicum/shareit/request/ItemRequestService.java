package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto create(Long userId, ItemRequestDto requestDto);
    ItemRequestWithItemsDto getById(Long userId, Long requestId);
    List<ItemRequestWithItemsDto> getAllByUser(Long userId);
    List<ItemRequestWithItemsDto> getAll(Long userId, Integer from, Integer size);
}
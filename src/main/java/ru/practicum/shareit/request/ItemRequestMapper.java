package ru.practicum.shareit.request;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;

@Component
public class ItemRequestMapper {

    public ItemRequestDto toDto(ItemRequest request) {
        if (request == null) {
            return null;
        }
        return new ItemRequestDto(
                request.getId(),
                request.getDescription(),
                request.getRequesterId(),
                request.getCreated()
        );
    }

    public ItemRequest toEntity(ItemRequestDto requestDto, Long requesterId) {
        if (requestDto == null) {
            return null;
        }
        ItemRequest request = new ItemRequest();
        request.setId(requestDto.getId());
        request.setDescription(requestDto.getDescription());
        request.setRequesterId(requesterId);
        request.setCreated(LocalDateTime.now());
        return request;
    }
}
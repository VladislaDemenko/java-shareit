package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final ItemRequestService requestService;

    @PostMapping
    public ItemRequestDto create(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @RequestBody ItemRequestDto requestDto) {
        log.info("POST /requests with userId: {}", userId);
        return requestService.create(userId, requestDto);
    }

    @GetMapping
    public List<ItemRequestWithItemsDto> getAllByUser(@RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("GET /requests with userId: {}", userId);
        return requestService.getAllByUser(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestWithItemsDto> getAll(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /requests/all with pagination");
        return requestService.getAll(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ItemRequestWithItemsDto getById(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @PathVariable Long requestId) {
        log.info("GET /requests/{}", requestId);
        return requestService.getById(userId, requestId);
    }
}
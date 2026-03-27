package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.ItemDto;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public ItemDto create(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @Valid @RequestBody ItemDto itemDto) {
        log.info("POST /items with userId: {}", userId);
        return itemService.create(userId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public ItemDto update(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @PathVariable Long itemId,
            @RequestBody ItemDto itemDto) {
        log.info("PATCH /items/{} with userId: {}", itemId, userId);

        if (itemId == null) {
            throw new IllegalArgumentException("ID вещи не может быть null");
        }

        return itemService.update(userId, itemId, itemDto);
    }

    @GetMapping("/{itemId}")
    public ItemDto getById(
            @RequestHeader(value = "X-Sharer-User-Id", required = false) Long userId,
            @PathVariable Long itemId) {
        log.info("GET /items/{} with userId: {}", itemId, userId);

        if (itemId == null) {
            throw new IllegalArgumentException("ID вещи не может быть null");
        }

        return itemService.getById(userId, itemId);
    }

    @GetMapping
    public List<ItemDto> getAllByOwner(@RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("GET /items with userId: {}", userId);
        return itemService.getAllByOwner(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestParam String text) {
        log.info("GET /items/search?text={}", text);
        return itemService.search(text);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(IllegalArgumentException e) {
        if (e.getMessage() != null &&
                (e.getMessage().contains("не найден") ||
                        e.getMessage().contains("не найдена") ||
                        e.getMessage().contains("только владелец"))) {
            log.error("Not found error: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
        throw e;
    }
}
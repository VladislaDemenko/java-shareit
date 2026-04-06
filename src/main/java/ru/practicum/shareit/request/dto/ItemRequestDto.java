package ru.practicum.shareit.request.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemRequestDto {
    Long id;

    @NotBlank(message = "Описание не может быть пустым")
    String description;

    Long requesterId;
    LocalDateTime created;
}
package ru.practicum.shareit.item.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentDto {

    private static final String TEXT_NOT_BLANK_MESSAGE = "Текст комментария не может быть пустым";
    private static final String TEXT_SIZE_MESSAGE = "Максимальная длина комментария 2000 символов";
    private static final int TEXT_MAX_SIZE = 2000;

    Long id;

    @NotBlank(message = TEXT_NOT_BLANK_MESSAGE)
    @Size(max = TEXT_MAX_SIZE, message = TEXT_SIZE_MESSAGE)
    String text;
}
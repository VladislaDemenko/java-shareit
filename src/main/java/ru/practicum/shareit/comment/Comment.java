package ru.practicum.shareit.comment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comments")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 2000)
    String text;

    @Column(name = "item_id", nullable = false)
    Long itemId;

    @Column(name = "author_id", nullable = false)
    Long authorId;

    @Column(name = "created_date", nullable = false)
    LocalDateTime created;
}
package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 1;

    @GetMapping
    public List<User> getAll() {
        log.info("GET /users");
        return new ArrayList<>(users.values());
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        log.info("GET /users/{}", id);
        User user = users.get(id);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
        return user;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody User user) {
        log.info("POST /users");

        if (users.values().stream().anyMatch(u -> u.getEmail().equals(user.getEmail()))) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        user.setId(currentId++);
        users.put(user.getId(), user);
        return user;
    }

    @PatchMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User userUpdates) {
        log.info("PATCH /users/{}", id);

        User existingUser = users.get(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        if (userUpdates.getEmail() != null && !userUpdates.getEmail().equals(existingUser.getEmail())) {
            if (users.values().stream().anyMatch(u -> u.getEmail().equals(userUpdates.getEmail()))) {
                throw new IllegalArgumentException("Пользователь с таким email уже существует");
            }
            existingUser.setEmail(userUpdates.getEmail());
        }

        if (userUpdates.getName() != null) {
            existingUser.setName(userUpdates.getName());
        }

        return existingUser;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        log.info("DELETE /users/{}", id);

        if (!users.containsKey(id)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        users.remove(id);
    }
}
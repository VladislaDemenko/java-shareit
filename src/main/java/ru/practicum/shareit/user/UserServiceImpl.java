package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final UserMapper mapper;

    @Override
    public UserDto create(UserDto userDto) {
        log.info("Creating user: {}", userDto);

        if (repository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        User user = mapper.toEntity(userDto);
        user = repository.save(user);

        return mapper.toDto(user);
    }

    @Override
    public UserDto update(Long id, UserDto userDto) {
        log.info("Updating user with id: {}", id);

        User existingUser = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (userDto.getEmail() != null && !userDto.getEmail().equals(existingUser.getEmail())) {
            if (repository.existsByEmail(userDto.getEmail())) {
                throw new IllegalArgumentException("Пользователь с таким email уже существует");
            }
            existingUser.setEmail(userDto.getEmail());
        }

        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }

        repository.save(existingUser);

        return mapper.toDto(existingUser);
    }

    @Override
    public UserDto getById(Long id) {
        log.info("Getting user by id: {}", id);

        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        return mapper.toDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        log.info("Getting all users");

        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting user with id: {}", id);

        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        repository.deleteById(id);
    }
}
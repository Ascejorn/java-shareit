package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import ru.practicum.shareit.handler.exception.NotFoundException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {
    private UserService userService;
    private UserRepository userRepository;

    private Long id;
    private Map<Long, User> usersInMemory;

    @BeforeEach
    void setUp() {
        id = 0L;
        usersInMemory = new HashMap<>();
        userRepository = Mockito.mock(UserRepository.class);
        Mockito.when(userRepository.save(ArgumentMatchers.any()))
                .then(invocation -> {
                    User user = invocation.getArgument(0);
                    if (user.getId() == null) {
                        user.setId(++id);
                    }
                    usersInMemory.put(user.getId(), user);
                    return user;
                });
        Mockito.when(userRepository.findById(ArgumentMatchers.anyLong()))
                .then(invocation -> {
                    Long id = invocation.getArgument(0);
                    return Optional.ofNullable(usersInMemory.get(id));
                });
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    @DisplayName("Create new user")
    void whenCreateNewUserByItemDto_returnNewUserDto() {
        UserDto user = UserDto.builder()
                .name("User Name")
                .email("user@email.com")
                .build();
        UserDto savedUser = userService.create(user);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo(user.getName());
        assertThat(savedUser.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("Get user by ID")
    void whenGetUserById_returnUserDto() {
        UserDto user = UserDto.builder()
                .name("User Name")
                .email("user@email.com")
                .build();
        UserDto savedUser = userService.create(user);
        UserDto loadedUser = userService.getById(savedUser.getId());
        assertThat(loadedUser).isNotNull();
        assertThat(loadedUser.getId()).isEqualTo(savedUser.getId());
        assertThat(loadedUser.getName()).isEqualTo(savedUser.getName());
        assertThat(loadedUser.getEmail()).isEqualTo(savedUser.getEmail());
    }

    @Test
    void getAll() {
        int userCount = 99;
        for (int i = 0; i < userCount; i++) {
            userService.create(UserDto.builder()
                    .name("User Name")
                    .email("user@email.com")
                    .build());
        }
        Mockito.when(userRepository.findAll())
                .thenReturn(new ArrayList<>(usersInMemory.values()));
        Collection<UserDto> users = userService.getAll();
        assertThat(users).isNotNull();
        assertThat(users.size()).isEqualTo(userCount);

    }

    @Test
    @DisplayName("Update user")
    void whenUpdateUserByItemDto_returnUserDto() {
        UserDto userDto = UserDto.builder()
                .name("User Name")
                .email("user@email.com")
                .build();
        UserDto savedUserDto = userService.create(userDto);
        UserDto newUserDto = UserDto.builder()
                .name("User Lastname")
                .email("update.user@email.com")
                .build();
        UserDto updatedUserDto = userService.update(newUserDto, savedUserDto.getId());
        assertThat(updatedUserDto).isNotNull();
        assertThat(updatedUserDto.getId()).isEqualTo(savedUserDto.getId());
        assertThat(updatedUserDto.getName()).isEqualTo(newUserDto.getName());
        assertThat(updatedUserDto.getEmail()).isEqualTo(newUserDto.getEmail());
    }

    @Test
    @DisplayName("Update user name")
    void whenUpdateUserName_returnUserDto() {
        UserDto userDto = UserDto.builder()
                .name("User Name")
                .email("user@email.com")
                .build();
        UserDto savedUserDto = userService.create(userDto);
        UserDto newUserDto = UserDto.builder()
                .name("User Lastname")
                .build();
        UserDto updatedUserDto = userService.update(newUserDto, savedUserDto.getId());
        assertThat(updatedUserDto).isNotNull();
        assertThat(updatedUserDto.getId()).isEqualTo(savedUserDto.getId());
        assertThat(updatedUserDto.getName()).isEqualTo(newUserDto.getName());
        assertThat(updatedUserDto.getEmail()).isEqualTo(savedUserDto.getEmail());
    }

    @Test
    @DisplayName("Update user email")
    void whenUpdateUserEmail_returnUserDto() {
        UserDto userDto = UserDto.builder()
                .name("User Name")
                .email("user@email.com")
                .build();
        UserDto savedUserDto = userService.create(userDto);
        UserDto newUserDto = UserDto.builder()
                .email("update.user@email.com")
                .build();
        UserDto updatedUserDto = userService.update(newUserDto, savedUserDto.getId());
        assertThat(updatedUserDto).isNotNull();
        assertThat(updatedUserDto.getId()).isEqualTo(savedUserDto.getId());
        assertThat(updatedUserDto.getName()).isEqualTo(savedUserDto.getName());
        assertThat(updatedUserDto.getEmail()).isEqualTo(newUserDto.getEmail());
    }

    @Test
    @DisplayName("Update new user with wrong ID")
    void whenUpdateUserWithWrongId_trow404Error() {
        UserDto newUserDto = UserDto.builder()
                .name("User Lastname")
                .email("update.user@email.com")
                .build();
        Long wrongId = 999L;
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.update(newUserDto, wrongId),
                "Not found user by id " + wrongId
        );
        assertThat(exception.getMessage()).isEqualTo("Not found user by id " + wrongId);
    }


    @Test
    void delete() {
        UserDto userDto = UserDto.builder()
                .name("User Name")
                .email("user@email.com")
                .build();
        UserDto savedUserDto = userService.create(userDto);
        userService.delete(savedUserDto.getId());
        User user = userService.getByIdOrNotFoundError(savedUserDto.getId());
        Mockito.verify(userRepository, Mockito.times(1))
                .delete(user);
    }
}
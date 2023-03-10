package ru.practicum.shareit.user.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.shareit.util.Create;
import ru.practicum.shareit.util.Update;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDto {
    @NotBlank(message = "Name/login is required.", groups = {Create.class})
    String name;

    @NotNull(message = "Email is required.", groups = {Create.class})
    @Email(message = "Email is invalid.", groups = {Create.class, Update.class})
    String email;
}

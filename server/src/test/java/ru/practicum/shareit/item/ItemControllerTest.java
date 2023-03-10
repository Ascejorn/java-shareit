package ru.practicum.shareit.item;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.dto.BookingStatus;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ItemControllerTest {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final BookingRepository bookingRepository;
    private final String userHeader = "X-Sharer-User-Id";
    private UserDto owner;

    @BeforeEach
    void setUp() throws Exception {
        String email = "owner" +  (int)(Math.random() * (9999) + 1) + "@mail.org";
        MvcResult mvcResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Current Owner\", " +
                                "\"email\": \"" + email + "\"}"))
                .andReturn();
        owner = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDto.class);
    }

    @Test
    @DisplayName("POST create item at /items")
    void whenCreateNewItemByItemDto_returnNewItemDto() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"name\": \"Wizard's Sorcery\", " +
                                "\"description\": \"Wand that allows you to defeat evil\", " +
                                "\"available\": true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").hasJsonPath())
                .andExpect(jsonPath("$.name").value("Wizard's Sorcery"))
                .andExpect(jsonPath("$.description").value("Wand that allows you to defeat evil"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("PATCH update item at /items/{id}")
    void whenUpdateItem_returnNewItemDto() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"name\": \"Wizard's Sorcery\", " +
                                "\"description\": \"Wand that allows you to defeat evil\", " +
                                "\"available\": true}"))
                .andReturn();
        ItemDto item = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ItemDto.class);
        mockMvc.perform(patch("/items/" + item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"id\": " + item.getId() + ", " +
                                "\"name\": \"Mr.Mouse\", " +
                                "\"description\": \"Micky Mouse toy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId()))
                .andExpect(jsonPath("$.name").value("Mr.Mouse"))
                .andExpect(jsonPath("$.description")
                        .value("Micky Mouse toy"))
                .andExpect(jsonPath("$.available").value(true));
        mockMvc.perform(patch("/items/" + item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"id\": " + item.getId() + ", " +
                                "\"available\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId()))
                .andExpect(jsonPath("$.name").value("Mr.Mouse"))
                .andExpect(jsonPath("$.description")
                        .value("Micky Mouse toy"))
                .andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(patch("/items/" + item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"id\": " + item.getId() + ", " +
                                "\"name\": \" \", " +
                                "\"description\": \" \", " +
                                "\"available\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId()))
                .andExpect(jsonPath("$.name").value("Mr.Mouse"))
                .andExpect(jsonPath("$.description")
                        .value("Micky Mouse toy"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("PATCH update item by wrong user at /items/{id}")
    void whenUpdateItemByWrongUser_throw403Error() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"name\": \"Wizard's Sorcery\", " +
                                "\"description\": \"Wand that allows you to defeat evil\", " +
                                "\"available\": true}"))
                .andReturn();
        ItemDto item = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ItemDto.class);
        mvcResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"User Name\", " +
                                "\"email\": \"user-879@mail.com\"}"))
                .andReturn();
        UserDto user = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDto.class);
        mockMvc.perform(patch("/items/" + item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, user.getId())
                        .content("{\"id\": " + item.getId() + ", " +
                                "\"name\": \"Mr.Mouse\", " +
                                "\"description\": \"Micky Mouse toy\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden item #" + item.getId()));
    }

    @Test
    @DisplayName("GET get item at /items/{id}")
    void whenRequestItemById_returnItemAdvancedDto() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"name\": \"Wizard's Sorcery\", " +
                                "\"description\": \"Wand that allows you to defeat evil\", " +
                                "\"available\": true}"))
                .andReturn();
        ItemDto item = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ItemDto.class);
        mockMvc.perform(get("/items/" + item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId()))
                .andExpect(jsonPath("$.name").value(item.getName()))
                .andExpect(jsonPath("$.description").value(item.getDescription()))
                .andExpect(jsonPath("$.lastBooking").hasJsonPath())
                .andExpect(jsonPath("$.nextBooking").hasJsonPath())
                .andExpect(jsonPath("$.available").value(item.getAvailable()))
                .andExpect(jsonPath("$.comments").hasJsonPath());
    }

    @Test
    @DisplayName("GET get item with wrong ID at /items/{id}")
    void whenRequestItemByWrongId_throw404Error() throws Exception {
        int wrongId = 9999;
        mockMvc.perform(get("/items/" + wrongId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found item #" + wrongId));
    }

    @Test
    @DisplayName("GET get items by owner ID at /items")
    void whenRequestItemsByOwnerId_returnListItemAdvancedDto() throws Exception {
        mockMvc.perform(post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .header(userHeader, owner.getId())
                .content("{\"name\": \"Other owner item\", " +
                        "\"description\": \"Other owner item description\", " +
                        "\"available\": true}"));
        setUp();
        int ownerItems = 5;
        for (int i = 0; i < ownerItems; i++) {
            mockMvc.perform(post("/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(userHeader, owner.getId())
                    .content("{\"name\": \"Item #" + i + "\", " +
                            "\"description\": \"Item #" + i + " description\", " +
                            "\"available\": true}"));
        }
        mockMvc.perform(get("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ownerItems));
    }

    @Test
    @DisplayName("GET search item at /items/search")
    void whenSearchRequest_returnListOfAvailableItems() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"name\": \"Harry's Wizard's Sorceries\", " +
                                "\"description\": \"Wand that allows you to defeat evil\", " +
                                "\"available\": true}"))
                .andReturn();
        ItemDto item1 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ItemDto.class);
        mvcResult = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"name\": \"Mr.Mouse\", " +
                                "\"description\": \"Harry's Micky Mouse toy\", " +
                                "\"available\": true}"))
                .andReturn();
        ItemDto item2 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ItemDto.class);
        String searchingText = "HaRry";
        mockMvc.perform(get("/items/search?text=" + searchingText)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].id").value(item1.getId()))
                .andExpect(jsonPath("$.[0].available").value(true))
                .andExpect(jsonPath("$.[1].id").value(item2.getId()))
                .andExpect(jsonPath("$.[1].available").value(true));
        searchingText = "SorCerieS";
        mockMvc.perform(get("/items/search?text=" + searchingText)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(item1.getId()))
                .andExpect(jsonPath("$.[0].available").value(true));
        searchingText = "tOy";
        mockMvc.perform(get("/items/search?text=" + searchingText)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(item2.getId()))
                .andExpect(jsonPath("$.[0].available").value(true));
        mockMvc.perform(get("/items/search?text=")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST create comment at /items/{id}/comment")
    void whenCreateNewComment_returnNewCommentDto() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, owner.getId())
                        .content("{\"name\": \"Wizard's Wand\", " +
                                "\"description\": \"Wand that allows you to defeat evil\", " +
                                "\"available\": true}"))
                .andReturn();
        Item item = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Item.class);
        mvcResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Comment Author\", " +
                                "\"email\": \"author@mail.org\"}"))
                .andReturn();
        User author = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), User.class);
        bookingRepository.save(Booking.builder()
                .created(LocalDateTime.now().minusDays(30))
                .status(BookingStatus.APPROVED)
                .start(LocalDateTime.now().minusDays(20))
                .end(LocalDateTime.now().minusDays(10))
                .item(item)
                .booker(author)
                .build());
        String text = "Do not pity the dead, Harry. " +
                "Pity the living, and, above all those who live without love.";
        mockMvc.perform(post("/items/" + item.getId() + "/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(userHeader, author.getId())
                        .content("{\"text\": \"" + text + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").hasJsonPath())
                .andExpect(jsonPath("$.text").value(text))
                .andExpect(jsonPath("$.authorName").value(author.getName()))
                .andExpect(jsonPath("$.created").hasJsonPath());
    }
}

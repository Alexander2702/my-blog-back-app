package ru.project.myBloc.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.project.myBloc.config.DataSourceConfig;
import ru.project.myBloc.config.WebConfig;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringJUnitConfig(classes = {
        DataSourceConfig.class,
        WebConfig.class
})
@WebAppConfiguration
@TestPropertySource(locations = "classpath:test-application.properties")
public class PostControllerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        // Чистим и наполняем БД перед каждым тестом
        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM post_tags");
        jdbcTemplate.execute("DELETE FROM posts");

        // Заполняем БД тестовыми данными перед каждым тестом
        jdbcTemplate.execute("""
                INSERT INTO posts (id, title, text, likes_count, image)
                VALUES (1, 'Путешествие на Урал', 'Описание маршрута по горам', 5, NULL)
                """);
        jdbcTemplate.execute("""
                INSERT INTO posts (id, title, text, likes_count, image)
                VALUES (2, 'Изучаем Spring MVC', 'Практика работы с JdbcTemplate', 1, NULL)
                """);

        jdbcTemplate.execute("INSERT INTO post_tags (post_id, tag) VALUES (1, 'ural'), (1, 'travel')");
        jdbcTemplate.execute("INSERT INTO post_tags (post_id, tag) VALUES (2, 'java')");
        jdbcTemplate.execute("INSERT INTO comments (id, text, post_id) VALUES (1, 'Отличный пост!', 1)");

        // Сдвигаем генераторы ID, чтобы новые записи не конфликтовали с тестовыми ID = 1 и 2
        jdbcTemplate.execute("ALTER TABLE posts ALTER COLUMN id RESTART WITH 3");
        jdbcTemplate.execute("ALTER TABLE comments ALTER COLUMN id RESTART WITH 2");
    }

    // Эндпоинт 1
    @Test
    void getPosts_returnsFilteredAndPagedJson() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "#ural Урал")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].id").isNumber())
                .andExpect(jsonPath("$.posts[0].title").value("Путешествие на Урал"))
                .andExpect(jsonPath("$.posts[0].tags", containsInAnyOrder("ural", "travel")))
                .andExpect(jsonPath("$.posts[0].likesCount").value(5))
                .andExpect(jsonPath("$.posts[0].commentsCount").value(1))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    //Эндпоинт 2
    @Test
    void getPostById_returnsFullPost() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Путешествие на Урал"))
                .andExpect(jsonPath("$.text").value("Описание маршрута по горам"));
    }

    @Test
    void getPostById_notFound_404() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    // Эндпоинт 3
    @Test
    void createPost_acceptsJson_andPersists() throws Exception {
        String json = """
                {
                  "title": "Новый пост",
                  "text": "Текст нового поста",
                  "tags": ["spring", "test"]
                }
                """;
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Новый пост"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("spring", "test")))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0));

        mockMvc.perform(get("/api/posts/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.title").value("Новый пост"))
                .andExpect(jsonPath("$.text").value("Текст нового поста"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("spring", "test")));
    }

    // Эндпоинт 4
    @Test
    void updatePost_updatesFieldsAndPreservesLikes() throws Exception {
        String json = """
                {
                  "id": 1,
                  "title": "Обновлённый Урал",
                  "text": "Новый текст",
                  "tags": ["ural"]
                }
                """;

        mockMvc.perform(put("/api/posts/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённый Урал"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("ural")))
                .andExpect(jsonPath("$.likesCount").value(5));
    }

    // Эндпоинт 5
    @Test
    void deletePost_removesPostAndCascadesComments() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", 1L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}", 1L))
                .andExpect(status().isNotFound());
    }

    // Эндпоинт 6
    @Test
    void addLike_incrementsLikesCount() throws Exception {
        mockMvc.perform(post("/api/posts/{id}/likes", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("6"));
    }

    // Эндпоинт 7
    @Test
    void uploadAndDownloadImage_success() throws Exception {
        byte[] pngStub = new byte[]{(byte) 137, 80, 78, 71};
        MockMultipartFile file = new MockMultipartFile("image", "post.png", "image/png", pngStub);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/posts/{id}/image", 1L).file(file))
                .andExpect(status().isOk());

        // Проверяем скачивание картинки и совпадение байтов
        mockMvc.perform(get("/api/posts/{id}/image", 1L))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngStub));
    }

    // Эндпоинт 8
    @Test
    void getImage_postHasNoImage_404() throws Exception {
        mockMvc.perform(get("/api/posts/{id}/image", 2L))
                .andExpect(status().isNotFound());
    }

}

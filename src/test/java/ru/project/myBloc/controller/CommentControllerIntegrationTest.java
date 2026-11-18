package ru.project.myBloc.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.project.myBloc.config.DataSourceConfig;
import ru.project.myBloc.config.WebConfig;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(classes = {
        DataSourceConfig.class,
        WebConfig.class
})
@WebAppConfiguration
@TestPropertySource(locations = "classpath:test-application.properties")
public class CommentControllerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        // Очищаем таблицы и наполняем тестовыми данными перед каждым тестом
        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM post_tags");
        jdbcTemplate.execute("DELETE FROM posts");

        jdbcTemplate.execute("""
                INSERT INTO posts (id, title, text, likes_count, image)
                VALUES (1, 'Тестовый пост', 'Текст поста', 0, NULL)
                """);
        jdbcTemplate.execute("INSERT INTO comments (id, text, post_id) VALUES (1, 'Первый комментарий', 1)");
        jdbcTemplate.execute("INSERT INTO comments (id, text, post_id) VALUES (2, 'Второй комментарий', 1)");

        // Сдвигаем счётчики автоинкремента, чтобы новые записи получали ID = 2 и 3
        jdbcTemplate.execute("ALTER TABLE posts ALTER COLUMN id RESTART WITH 2");
        jdbcTemplate.execute("ALTER TABLE comments ALTER COLUMN id RESTART WITH 3");
    }

    // Эндпоинт 9
    @Test
    void getComments_returnsJsonArray() throws Exception {
        mockMvc.perform(get("/api/posts/{id}/comments", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].text").value("Первый комментарий"))
                .andExpect(jsonPath("$[1].text").value("Второй комментарий"));
    }

    // Эндпоинт 10
    @Test
    void getCommentById_returnsSingleComment() throws Exception {
        mockMvc.perform(get("/api/posts/{id}/comments/{commentId}", 1L, 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.text").value("Первый комментарий"))
                .andExpect(jsonPath("$.postId").value(1));
    }

    // Эндпоинт 11
    @Test
    void createComment_acceptsJson_andPersists() throws Exception {
        String json = """
                {
                  "text": "Третий комментарий",
                  "postId": 1
                }
                """;

        mockMvc.perform(post("/api/posts/{id}/comments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.text").value("Третий комментарий"))
                .andExpect(jsonPath("$.postId").value(1));

        // убеждаемся, что комментариев в БД стало 3
        mockMvc.perform(get("/api/posts/{id}/comments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    // Эндпоинт 12
    @Test
    void updateComment_updatesText_andPersists() throws Exception {
        String json = """
                {
                  "id": 1,
                  "text": "Обновлённый комментарий",
                  "postId": 1
                }
                """;

        mockMvc.perform(put("/api/posts/{id}/comments/{commentId}", 1L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Обновлённый комментарий"));

        // убеждаемся, что новый текст сохранился в БД
        mockMvc.perform(get("/api/posts/{id}/comments/{commentId}", 1L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Обновлённый комментарий"));
    }

    @Test
    void deleteComment_removesComment() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}/comments/{commentId}", 1L, 1L))
                .andExpect(status().isOk());

        // убеждаемся, что в БД остался только 1 комментарий
        mockMvc.perform(get("/api/posts/{id}/comments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}

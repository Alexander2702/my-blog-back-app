package ru.project.myBloc.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.project.myBloc.model.Post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPostRepositoryBean implements PostRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPostRepositoryBean(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Маппер строки результата SQL-запроса в доменную сущность Post
    private static final RowMapper<Post> POST_ROW_MAPPER = (rs, rowNum) -> Post.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .text(rs.getString("text"))
            .likesCount(rs.getInt("likes_count"))
            .commentsCount(rs.getInt("comments_count"))
            .tags(new ArrayList<>())
            .build();

    @Override
    public List<Post> findPosts(String titleSubstring, List<String> tags, int limit, int offset) {
        List<Object> params = new ArrayList<>();
        // Динамически формируем блок WHERE и наполняем список позиционных параметров
        String whereClause =
                buildWhereClause(titleSubstring, tags, params);

        params.add(limit);
        params.add(offset);

        // Выбираем посты для текущей страницы (свежие сверху) и считаем количество комментариев
        String sql = """
                SELECT p.id, p.title, p.text, p.likes_count,
                       (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments_count
                FROM posts p
                """ + whereClause + """
                ORDER BY p.id DESC
                LIMIT ? OFFSET ?
                """;

        List<Post> posts = jdbcTemplate.query(sql, POST_ROW_MAPPER, params.toArray());
        // Подгружаем теги для всех найденных постов одним дополнительным запросом
        loadTagsForPosts(posts);
        return posts;
    }

    @Override
    public long countPosts(String titleSubstring, List<String> tags) {
        List<Object> params = new ArrayList<>();
        String whereClause = buildWhereClause(titleSubstring, tags, params);

        // Считаем общее число постов по тем же условиям фильтрации (для расчёта lastPage)
        String sql = "SELECT COUNT(*) FROM posts p " + whereClause;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public Optional<Post> findById(long id) {
        return Optional.empty();
    }

    @Override
    public Post save(Post post) {
        return null;
    }

    @Override
    public void update(Post post) {

    }

    // Формирует SQL-условие WHERE на основе подстроки заголовка и списка обязательных тегов
    private String buildWhereClause(String titleSubstring, List<String> tags, List<Object> params) {
        List<String> conditions = new ArrayList<>();

        // Фильтрация по вхождению подстроки в заголовок без учёта регистра
        if (titleSubstring != null && !titleSubstring.isBlank()) {
            conditions.add("LOWER(p.title) LIKE ?");
            params.add("%" + titleSubstring.toLowerCase() + "%");
        }

        // Фильтрация по тегам по принципу "И": генерируем нужное количество '?' для оператора IN
        if (tags != null && !tags.isEmpty()) {
            String placeholders = String.join(", ", Collections.nCopies(tags.size(), "?"));
            conditions.add("""
                    (SELECT COUNT(DISTINCT pt.tag)
                     FROM post_tags pt
                     WHERE pt.post_id = p.id AND pt.tag IN (%s)) = ?
                    """.formatted(placeholders));
            params.addAll(tags);
            params.add(tags.size());
        }

        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions) + " ";
    }

    // Пакетно загружает теги из таблицы post_tags для списка постов
    private void loadTagsForPosts(List<Post> posts) {
        if (posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        String placeholders = String.join(", ", Collections.nCopies(postIds.size(), "?"));
        String sql = "SELECT post_id, tag FROM post_tags WHERE post_id IN (" + placeholders + ") ORDER BY tag";

        Map<Long, List<String>> tagsByPostId = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            long postId = rs.getLong("post_id");
            String tag = rs.getString("tag");
            tagsByPostId.computeIfAbsent(postId, k -> new ArrayList<>()).add(tag);
        }, postIds.toArray());

        // Распределяем загруженные теги по соответствующим объектам Post
        for (Post post : posts) {
            post.setTags(tagsByPostId.getOrDefault(post.getId(), new ArrayList<>()));
        }
    }
}

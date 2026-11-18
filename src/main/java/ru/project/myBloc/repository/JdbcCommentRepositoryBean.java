package ru.project.myBloc.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.project.myBloc.model.Comment;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcCommentRepositoryBean implements CommentRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCommentRepositoryBean(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Comment> findAllByPostId(long postId) {
        String sql = "SELECT id, text, post_id FROM comments WHERE post_id = ? ORDER BY id ASC";

        // Маппим каждую строку таблицы comments в доменную сущность Comment
        return jdbcTemplate.query(sql, (rs, rowNum) -> Comment.builder()
                .id(rs.getLong("id"))
                .text(rs.getString("text"))
                .postId(rs.getLong("post_id"))
                .build(), postId);
    }

    @Override
    public Optional<Comment> findByIdAndPostId(long commentId, long postId) {
        String sql = "SELECT id, text, post_id FROM comments WHERE id = ? AND post_id = ?";

        List<Comment> comments = jdbcTemplate.query(sql, (rs, rowNum) -> Comment.builder()
                .id(rs.getLong("id"))
                .text(rs.getString("text"))
                .postId(rs.getLong("post_id"))
                .build(), commentId, postId);

        if (comments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(comments.getFirst());
    }

    @Override
    public Comment save(Comment comment) {
        String sql = "INSERT INTO comments (text, post_id) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // Выполняем вставку комментария и запрашиваем у БД id
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, comment.getText());
            ps.setLong(2, comment.getPostId());
            return ps;
        }, keyHolder);

        // Проставляем id в объект комментария
        long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        comment.setId(generatedId);
        return comment;
    }

    @Override
    public void update(Comment comment) {
        jdbcTemplate.update(
                "UPDATE comments SET text = ? WHERE id = ? AND post_id = ?",
                comment.getText(), comment.getId(), comment.getPostId()
        );
    }

    @Override
    public void deleteByIdAndPostId(long commentId, long postId) {
        jdbcTemplate.update(
                "DELETE FROM comments WHERE id = ? AND post_id = ?",
                commentId, postId
        );
    }

}

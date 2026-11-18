package ru.project.myBloc.repository;

import ru.project.myBloc.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {

    // Возвращает список всех комментариев для указанного поста
    List<Comment> findAllByPostId(long postId);

    // Находит конкретный комментарий по его ID и ID поста
    Optional<Comment> findByIdAndPostId(long commentId, long postId);

    // Сохраняет новый комментарий в БД и возвращает его с присвоенным ID
    Comment save(Comment comment);

    // Обновляет текст существующего комментария по его ID и ID поста
    void update(Comment comment);

    // Удаляет комментарий по его ID и ID поста
    void deleteByIdAndPostId(long commentId, long postId);
}

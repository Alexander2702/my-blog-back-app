package ru.project.myBloc.repository;

import ru.project.myBloc.model.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

    // Возвращает страницу постов с учётом фильтра по подстроке названия и списку тегов
    List<Post> findPosts(String titleSubstring, List<String> tags, int limit, int offset);

    // Подсчитывает общее количество постов, удовлетворяющих критериям поиска
    long countPosts(String titleSubstring, List<String> tags);

    // Находит пост по ID вместе со списком тегов и количеством комментариев
    Optional<Post> findById(long id);

    // Сохраняет новый пост и возвращает его с присвоенным ID
    Post save(Post post);

    // Обновляет существующий пост и его теги
    void update(Post post);

    // Удаляет пост по ID (связанные теги и комментарии удалятся каскадно)
    void deleteById(long id);

}

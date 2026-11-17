package ru.project.myBloc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.project.myBloc.dto.PostRequestDto;
import ru.project.myBloc.dto.PostResponseDto;
import ru.project.myBloc.dto.PostsPageResponseDto;
import ru.project.myBloc.model.Post;
import ru.project.myBloc.repository.PostRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceBeanTest {
    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostServiceBean postService;

    @Test
    void getPosts_parsesSearchAndTruncatesLongText() {
        // пост с текстом длиннее 128 символов
        String str = "A".repeat(150);
        Post post = Post.builder()
                .id(1L)
                .title("Пишем тесты для поста")
                .text(str)
                .tags(List.of("create", "test"))
                .likesCount(5)
                .commentsCount(3)
                .build();
        //Настройка поведения mock-объекта - если сервис правильно разберёт поисковую строку
        // на заголовок "тесты" и теги ["create", "test"], мок вернёт 6 найденных постов и объект post
        when(postRepository.countPosts("тесты", List.of("create", "test"))).thenReturn(6L);
        when(postRepository.findPosts("тесты", List.of("create", "test"), 5, 0))
                .thenReturn(List.of(post));

        // Передаём "грязную" строку поиска с лишними пробелами, первую страницу и размер 5 постов
        PostsPageResponseDto result =
                postService.getPosts("  #create   тесты   #test  ", 1, 5);

        // Проверяем расчёт пагинации (6 постов по 5 на страницу = 2 страницы, текущая 1-я)
        assertEquals(2, result.getLastPage());
        assertFalse(result.isHasPrev());
        assertTrue(result.isHasNext());

        // Проверяем обрезку длинного текста до 128 символов + символ '…' (итого 129 символов)
        assertEquals(1, result.getPosts().size());
        PostResponseDto dto = result.getPosts().getFirst();
        assertEquals(129, dto.getText().length());
        assertTrue(dto.getText().endsWith("…"));
    }

    @Test
    void getPostById_returnsFullTextWithoutTruncation() {
        // Создаём строку длиной 150 символов (больше 128)
        String str = "A".repeat(150);
        Post post = Post.builder()
                .id(10L)
                .title("Полный пост")
                .text(str)
                .tags(List.of("java"))
                .likesCount(4)
                .commentsCount(1)
                .build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        PostResponseDto dto = postService.getPostById(10L);
        // Проверяем, что на странице одного поста текст НЕ обрезается до 128 символов, а отдаётся целиком
        assertEquals(10L, dto.getId());
        assertEquals(150, dto.getText().length());
        assertEquals(str, dto.getText());
    }

    @Test
    void getPostById_throwsNotFoundWhenMissing() {
        // Имитируем ситуацию, когда поста с ID = 999 нет в бд
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        // Ожидаем, что сервис выбросит ResponseStatusException со статусом 404 Not Found
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> postService.getPostById(999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createPost_sanitizesTagsAndSaves() {
        // Формируем запрос с "грязными" тегами: пробелы по краям, дубликат "java" и пустая строка
        PostRequestDto dto = PostRequestDto.builder()
                .title("New post")
                .text("set text for new post")
                .tags(List.of("  java ", "spring", "java", "  "))
                .build();
        // Имитируем работу БД: когда репозиторий вызовет save(post), возвращаем этот же объект,
        // но с присвоенным идентификатором id = 5L
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post postToSave = invocation.getArgument(0);
            postToSave.setId(5L);
            return postToSave;
        });

        PostResponseDto created = postService.createPost(dto);

        // Проверяем, что вернулся ID, присвоенный репозиторием, и корректные поля
        assertEquals(5L, created.getId());
        assertEquals("New post", created.getTitle());
        assertEquals("set text for new post", created.getText());

        // Проверяем работу sanitizeTags: дубликат и пробелы удалены, остались только ["java", "spring"]
        assertEquals(List.of("java", "spring"), created.getTags());

        // Проверяем, что метод save в репозитории действительно был вызван 1 раз
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void updatePost_updatesFieldsAndPreservesCounters() {
        // Имитируем существующий пост в БД с 10 лайками и 3 комментариями
        Post existingPost = Post.builder()
                .id(3L)
                .title("Старый заголовок")
                .text("Старый текст")
                .tags(List.of("old"))
                .likesCount(10)
                .commentsCount(3)
                .build();

        PostRequestDto request = PostRequestDto.builder()
                .id(3L)
                .title("Новый заголовок")
                .text("Новый текст")
                .tags(List.of(" java ", "java", "spring"))
                .build();

        when(postRepository.findById(3L)).thenReturn(Optional.of(existingPost));

        PostResponseDto updated = postService.updatePost(3L, request);

        // Проверяем, что заголовок, текст и теги обновились и очистились
        assertEquals(3L, updated.getId());
        assertEquals("Новый заголовок", updated.getTitle());
        assertEquals("Новый текст", updated.getText());
        assertEquals(List.of("java", "spring"), updated.getTags());

        // Проверяем главное правило: лайки (10) и комментарии (3) не сбросились
        assertEquals(10, updated.getLikesCount());
        assertEquals(3, updated.getCommentsCount());

        // Проверяем, что репозиторий получил команду на обновление в БД
        verify(postRepository).update(any(Post.class));
    }

    @Test
    void updatePost_throwsNotFoundWhenMissing() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        PostRequestDto request = new PostRequestDto();
        request.setTitle("Заголовок");
        request.setText("Текст");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> postService.updatePost(999L, request)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}

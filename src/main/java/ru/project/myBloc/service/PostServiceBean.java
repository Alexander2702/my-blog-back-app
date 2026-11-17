package ru.project.myBloc.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.project.myBloc.dto.PostRequestDto;
import ru.project.myBloc.dto.PostResponseDto;
import ru.project.myBloc.dto.PostsPageResponseDto;
import ru.project.myBloc.model.Post;
import ru.project.myBloc.repository.PostRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceBean implements PostService {
    // Максимальная длина текста поста в ленте перед обрезкой
    private static final int PREVIEW_MAX_LENGTH = 128;

    private final PostRepository postRepository;

    public PostServiceBean(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    // Получение списка постов для ленты с поиском и пагинацией
    public PostsPageResponseDto getPosts(String search, int pageNumber, int pageSize) {
        // Разбираем строку поиска на теги (слова с '#') и подстроку заголовка
        List<String> tags = new ArrayList<>();
        String titleSubstring = parseSearch(search, tags);

        // Считаем общее количество подходящих постов и вычисляем номер последней страницы
        long totalPosts = postRepository.countPosts(titleSubstring, tags);
        int lastPage = Math.max(1, (int) Math.ceil((double) totalPosts / pageSize));
        int offset = (pageNumber - 1) * pageSize;

        // Получаем посты из БД для текущей страницы и конвертируем их в DTO с обрезкой текста
        List<Post> posts = postRepository.findPosts(titleSubstring, tags, pageSize, offset);
        List<PostResponseDto> postDtos =
                posts.stream().map(this::toPreviewDto).collect(Collectors.toList());

        // 4. Собираем итоговый ответ с флагами навигации по страницам
        return PostsPageResponseDto.builder()
                .posts(postDtos)
                .hasPrev(pageNumber > 1)
                .hasNext(pageNumber < lastPage)
                .lastPage(lastPage)
                .build();
    }

    // Получение полной информации о посте по ID
    @Override
    public PostResponseDto getPostById(long id) {
        // Ищем пост в БД; если не найден — возвращаем статус 404 Not Found
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден: " + id));

        // Возвращаем полное DTO (поле text не обрезается до 128 символов)
        return toFullDto(post);
    }

    // Создание нового поста
    @Override
    @Transactional
    public PostResponseDto createPost(PostRequestDto request) {
        // Очищаем список тегов через отдельный вспомогательный метод
        List<String> cleanTags = sanitizeTags(request.getTags());

        // Собираем новую сущность Post и сохраняем в БД
        Post post = Post.builder()
                .title(request.getTitle())
                .text(request.getText())
                .tags(cleanTags)
                .build();

        Post savedPost = postRepository.save(post);
        // Возвращаем полный JSON созданного поста
        return toFullDto(savedPost);
    }

    @Override
    @Transactional
    public PostResponseDto updatePost(long id, PostRequestDto request) {
        // Проверяем, что пост существует в БД, и получаем его актуальные likesCount и commentsCount
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден: " + id));

        // Обновляем поля поста и очищаем новый список тегов
        Post updatedPost = existingPost.toBuilder()
                .title(request.getTitle())
                .text(request.getText())
                .tags(sanitizeTags(request.getTags()))
                .build();

        // Сохраняем изменения в БД и возвращаем полное DTO
        postRepository.update(updatedPost);
        return toFullDto(updatedPost);
    }

    @Override
    @Transactional
    public void deletePost(long id) {
        checkPostExists(id);

        postRepository.deleteById(id);
    }

    // Увеличение счётчика лайков поста на +1
    @Override
    @Transactional
    public int addLike(long id) {
        checkPostExists(id);

        return postRepository.incrementLikes(id);
    }

    // Загрузка или обновление картинки поста
    @Override
    @Transactional
    public void updatePostImage(long id, MultipartFile image) {
        checkPostExists(id);

        try {
            // Извлекаем массив байт из загруженного файла и сохраняем в БД
            byte[] imageBytes = image.getBytes();
            postRepository.updateImage(id, imageBytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось прочитать файл изображения", e);
        }
    }

    @Override
    public byte[] getPostImage(long id) {
        // Извлекаем байты картинки из БД; если пост или картинка отсутствуют — возвращаем 404 Not Found
        return postRepository.findImageById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Картинка не найдена для поста: " + id));
    }

    @Override
    public void checkPostExists(long postId) {
        if (postRepository.findById(postId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден: " + postId);
        }
    }

    // очищает список тегов от null, пробелов и дубликатов
    private List<String> sanitizeTags(List<String> tags) {
        List<String> cleanTags = new ArrayList<>();
        if (tags == null) {
            return cleanTags;
        }
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                String trimmed = tag.trim();
                if (!cleanTags.contains(trimmed)) {
                    cleanTags.add(trimmed);
                }
            }
        }
        return cleanTags;
    }

    // Преобразует сущность Post в полное DTO для страницы поста
    private PostResponseDto toFullDto(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .text(post.getText())
                .tags(post.getTags())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .build();
    }

    // Преобразует сущность Post в DTO для ленты: если текст длиннее 128 символов, обрезает и добавляет '…'
    private PostResponseDto toPreviewDto(Post post) {
        String text = post.getText();
        if (text != null && text.length() > PREVIEW_MAX_LENGTH) {
            text = text.substring(0, PREVIEW_MAX_LENGTH) + "…";
        }

        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .text(text) // текст не полный
                .tags(post.getTags())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .build();
    }

    // Разбивает строку search по пробелам: теги с '#' идут в список tags, остальные слова склеиваются через пробел
    private String parseSearch(String search, List<String> tags) {
        if (search == null || search.isBlank()) {
            return "";
        }

        String[] words = search.split(" ");
        List<String> titleWords = new ArrayList<>();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.startsWith("#")) {
                String tag = word.substring(1);
                if (!tag.isEmpty() && !tags.contains(tag)) {
                    tags.add(tag);
                }
            } else {
                titleWords.add(word);
            }
        }

        return String.join(" ", titleWords);
    }
}

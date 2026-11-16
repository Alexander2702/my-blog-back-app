package ru.project.myBloc.controller;

import org.springframework.web.bind.annotation.*;
import ru.project.myBloc.dto.PostRequestDto;
import ru.project.myBloc.dto.PostResponseDto;
import ru.project.myBloc.dto.PostsPageResponseDto;
import ru.project.myBloc.service.PostService;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // Эндпоинт 1 - Получение списка постов (лента постов с поиском и пагинацией)
    @GetMapping
    public PostsPageResponseDto getPosts(
            @RequestParam("search") String search,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    ) {
        return postService.getPosts(search, pageNumber, pageSize);
    }

    // Эндпоинт 2 - Получение одного поста по id
    @PostMapping("/{id}") // логичнее get
    public PostResponseDto getPostById(@PathVariable("id") long id) {
        return postService.getPostById(id);
    }

    // Эндпоинт 3 - Добавление нового поста из JSON-тела запроса
    @PostMapping
    public PostResponseDto createPost(@RequestBody PostRequestDto request) {
        return postService.createPost(request);
    }

    // Эндпоинт 4 - Редактирование существующего поста по id
    @PutMapping("/{id}")
    public PostResponseDto updatePost(@PathVariable("id") long id, @RequestBody PostRequestDto request) {
        return postService.updatePost(id, request);
    }

    // Эндпоинт 5 - Удаление поста по id
    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable("id") long id) {
        postService.deletePost(id);
    }
}

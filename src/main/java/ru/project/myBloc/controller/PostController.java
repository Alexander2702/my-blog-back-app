package ru.project.myBloc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
}

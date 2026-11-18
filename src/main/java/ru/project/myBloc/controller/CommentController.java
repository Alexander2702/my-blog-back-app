package ru.project.myBloc.controller;

import org.springframework.web.bind.annotation.*;
import ru.project.myBloc.dto.CommentDto;
import ru.project.myBloc.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{id}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // Эндпоинт 9 - Получение списка комментариев к посту
    @GetMapping
    public List<CommentDto> getComments(@PathVariable("id") long postId) {
        return commentService.getCommentsByPostId(postId);
    }

    // Эндпоинт 10 - Получение одного комментария по ID поста и ID комментария
    @GetMapping("/{commentId}")
    public CommentDto getCommentById(
            @PathVariable("id") long postId,
            @PathVariable("commentId") long commentId
    ) {
        return commentService.getCommentById(postId, commentId);
    }

    // Эндпоинт 11 - Добавление нового комментария к посту
    @PostMapping
    public CommentDto createComment(
            @PathVariable("id") long postId,
            @RequestBody CommentDto request
    ) {
        return commentService.createComment(postId, request);
    }

    // Эндпоинт 12 - Редактирование комментария по ID поста и ID комментария
    @PutMapping("/{commentId}")
    public CommentDto updateComment(
            @PathVariable("id") long postId,
            @PathVariable("commentId") long commentId,
            @RequestBody CommentDto request
    ) {
        return commentService.updateComment(postId, commentId, request);
    }

    // Эндпоинт 13- Удаление комментария по ID поста и ID комментария
    @DeleteMapping("/{commentId}")
    public void deleteComment(
            @PathVariable("id") long postId,
            @PathVariable("commentId") long commentId
    ) {
        commentService.deleteComment(postId, commentId);
    }
}

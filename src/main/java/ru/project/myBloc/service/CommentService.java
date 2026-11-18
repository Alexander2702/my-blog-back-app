package ru.project.myBloc.service;

import ru.project.myBloc.dto.CommentDto;

import java.util.List;

public interface CommentService {

    List<CommentDto> getCommentsByPostId(long postId);

    CommentDto getCommentById(long postId, long commentId);

    CommentDto createComment(long postId, CommentDto request);

    CommentDto updateComment(long postId, long commentId, CommentDto request);

    void deleteComment(long postId, long commentId);
}

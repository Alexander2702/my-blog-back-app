package ru.project.myBloc.service;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.project.myBloc.dto.CommentDto;
import ru.project.myBloc.model.Comment;
import ru.project.myBloc.repository.CommentRepository;

import java.util.List;

public class CommentServiceBean implements CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;

    public CommentServiceBean(CommentRepository commentRepository, PostService postService) {
        this.commentRepository = commentRepository;
        this.postService = postService;
    }

    @Override
    public List<CommentDto> getCommentsByPostId(long postId) {
        postService.checkPostExists(postId);

        List<Comment> comments = commentRepository.findAllByPostId(postId);

        return comments.stream().map(this::toDto).toList();
    }

    @Override
    public CommentDto getCommentById(long postId, long commentId) {
        postService.checkPostExists(postId);

        // Ищем комментарий, принадлежащий этому посту; если не найден — возвращаем 404 Not Found
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Комментарий " + commentId + " не найден для поста " + postId
                ));

        return toDto(comment);
    }

    @Override
    @Transactional
    public CommentDto createComment(long postId, CommentDto request) {
        postService.checkPostExists(postId);

        Comment comment = Comment.builder()
                .text(request.getText())
                .postId(postId)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(long postId, long commentId, CommentDto request) {
        postService.checkPostExists(postId);

        // Ищем комментарий у этого поста; если не найден — возвращаем 404 Not Found
        Comment existingComment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Комментарий " + commentId + " не найден для поста " + postId
                ));
        existingComment.setText(request.getText());
        commentRepository.update(existingComment);

        return toDto(existingComment);
    }

    @Override
    @Transactional
    public void deleteComment(long postId, long commentId) {
        postService.checkPostExists(postId);

        if (commentRepository.findByIdAndPostId(commentId, postId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Комментарий " + commentId + " не найден для поста " + postId
            );
        }

        commentRepository.deleteByIdAndPostId(commentId, postId);
    }

    private CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .postId(comment.getPostId())
                .build();
    }
}

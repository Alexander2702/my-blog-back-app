package ru.project.myBloc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.project.myBloc.dto.CommentDto;
import ru.project.myBloc.model.Comment;
import ru.project.myBloc.repository.CommentRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceBeanTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostService postService;
    @InjectMocks
    private CommentServiceBean commentService;

    @Test
    void getCommentsByPostId_returnsMappedListWhenPostExists() {
        Comment c1 = Comment.builder().id(1L).text("Первый комментарий").postId(10L).build();
        Comment c2 = Comment.builder().id(2L).text("Второй комментарий").postId(10L).build();

        doNothing().when(postService).checkPostExists(10L);
        when(commentRepository.findAllByPostId(10L)).thenReturn(List.of(c1, c2));

        List<CommentDto> result = commentService.getCommentsByPostId(10L);

        assertEquals(2, result.size());
        assertEquals(1L, result.getFirst().getId());
        assertEquals("Первый комментарий", result.get(0).getText());
        assertEquals(10L, result.get(0).getPostId());
        assertEquals("Второй комментарий", result.get(1).getText());
    }

    @Test
    void getCommentsByPostId_throwsNotFoundWhenPostMissing() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден"))
                .when(postService).checkPostExists(999L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> commentService.getCommentsByPostId(999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        // Проверяем, что при отсутствии поста сервис не обращался к таблице комментариев
        verifyNoInteractions(commentRepository);
    }

    // проверяем, что если пост и комментарий существуют, сервис корректно возвращает CommentDto
    @Test
    void getCommentById_returnsDtoWhenFound() {
        Comment comment = Comment.builder().id(2L).text("Текст комментария").postId(10L).build();
        doNothing().when(postService).checkPostExists(10L);
        when(commentRepository.findByIdAndPostId(2L, 10L)).thenReturn(Optional.of(comment));

        CommentDto result = commentService.getCommentById(10L, 2L);

        assertEquals(2L, result.getId());
        assertEquals("Текст комментария", result.getText());
        assertEquals(10L, result.getPostId());
    }

    @Test
    void createComment_savesAndReturnsDto() {
        doNothing().when(postService).checkPostExists(10L);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment toSave = invocation.getArgument(0);
            toSave.setId(5L);
            return toSave;
        });

        CommentDto request = new CommentDto();
        request.setText("Новый комментарий");

        CommentDto created = commentService.createComment(10L, request);

        assertEquals(5L, created.getId());
        assertEquals(10L, created.getPostId());
        assertEquals("Новый комментарий", created.getText());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void updateComment_updatesTextAndSaves() {
        Comment existing = Comment.builder().id(2L).text("Старый текст").postId(10L).build();
        doNothing().when(postService).checkPostExists(10L);
        when(commentRepository.findByIdAndPostId(2L, 10L)).thenReturn(Optional.of(existing));

        CommentDto request = new CommentDto();
        request.setId(2L);
        request.setText("Обновлённый текст");
        request.setPostId(10L);

        CommentDto updated = commentService.updateComment(10L, 2L, request);

        assertEquals("Обновлённый текст", updated.getText());
        verify(commentRepository).update(existing);
    }

    @Test
    void deleteComment_throwsNotFoundWhenCommentMissing() {
        doNothing().when(postService).checkPostExists(10L);
        when(commentRepository.findByIdAndPostId(999L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> commentService.deleteComment(10L, 999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        // Проверяем, что команда на удаление в БД не отправлялась
        verify(commentRepository, never()).deleteByIdAndPostId(999L, 10L);
    }

}

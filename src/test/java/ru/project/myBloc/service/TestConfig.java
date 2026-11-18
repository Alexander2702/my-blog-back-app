package ru.project.myBloc.service;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import ru.project.myBloc.repository.CommentRepository;
import ru.project.myBloc.repository.PostRepository;

@ComponentScan("ru.project.myBloc.service")
public class TestConfig {

    @Bean
    @Primary
    public PostRepository mockPostRepository() {
        return Mockito.mock(PostRepository.class);
    }

    @Bean
    @Primary
    public CommentRepository mockCommentRepository() {
        return Mockito.mock(CommentRepository.class);
    }

    @Bean
    @Primary
    public PostService mockPostService() {
        return Mockito.mock(PostService.class);
    }
}

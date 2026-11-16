package ru.project.myBloc.service;


import ru.project.myBloc.dto.PostRequestDto;
import ru.project.myBloc.dto.PostResponseDto;
import ru.project.myBloc.dto.PostsPageResponseDto;

public interface PostService {

    PostsPageResponseDto getPosts(String search, int pageNumber, int pageSize);

    PostResponseDto getPostById(long id);

    PostResponseDto createPost(PostRequestDto request);

    PostResponseDto updatePost(long id, PostRequestDto request);

    void deletePost(long id);
}

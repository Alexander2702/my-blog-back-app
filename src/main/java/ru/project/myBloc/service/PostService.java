package ru.project.myBloc.service;


import ru.project.myBloc.dto.PostsPageResponseDto;

public interface PostService {

    PostsPageResponseDto getPosts(String search, int pageNumber, int pageSize);
}

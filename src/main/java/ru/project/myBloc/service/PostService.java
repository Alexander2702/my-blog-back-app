package ru.project.myBloc.service;


import org.springframework.web.multipart.MultipartFile;
import ru.project.myBloc.dto.PostRequestDto;
import ru.project.myBloc.dto.PostResponseDto;
import ru.project.myBloc.dto.PostsPageResponseDto;

public interface PostService {

    PostsPageResponseDto getPosts(String search, int pageNumber, int pageSize);

    PostResponseDto getPostById(long id);

    PostResponseDto createPost(PostRequestDto request);

    PostResponseDto updatePost(long id, PostRequestDto request);

    void deletePost(long id);

    int addLike(long id);

    void updatePostImage(long id, MultipartFile image);

    byte[] getPostImage(long id);

    void checkPostExists(long postId);
}

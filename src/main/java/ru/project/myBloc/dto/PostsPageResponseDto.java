package ru.project.myBloc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO для ответа ленты постов
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostsPageResponseDto {

    private List<PostResponseDto> posts;
    private boolean hasPrev;
    private boolean hasNext;
    private int lastPage;
}

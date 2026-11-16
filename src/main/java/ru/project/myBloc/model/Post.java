package ru.project.myBloc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private Long id;
    private String title;
    private String text;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private int likesCount;
    private int commentsCount;
    private byte[] image;

}

package ru.project.myBloc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// DTO для приёма данных при создании нового поста (POST /api/posts)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDto {

    private Long id;
    private String title;
    private String text;
    private List<String> tags = new ArrayList<>();
}

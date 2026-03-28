package com.app.codemasterpiecebackend.domain.category.dto;

import com.app.codemasterpiecebackend.domain.category.entity.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private String categoryId;
    private String name;
    private CategoryType type;
    private int sortOrder;
    private int level;
    private String link;
    private String imagePath;
    private List<CategoryDTO> children;
}

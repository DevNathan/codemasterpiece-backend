package com.app.codemasterpiecebackend.domain.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostTocDTO {
    String id;
    String text;
    int depth;
}

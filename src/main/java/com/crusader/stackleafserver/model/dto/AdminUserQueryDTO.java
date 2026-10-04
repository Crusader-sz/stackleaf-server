package com.crusader.stackleafserver.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminUserQueryDTO {
    @NotNull
    @Min(1)
    private Integer pageNum = 1;
    @NotNull
    @Min(1) @Max(100)
    private Integer pageSize = 10;
    @Size(max = 50)
    private String keyword;
}

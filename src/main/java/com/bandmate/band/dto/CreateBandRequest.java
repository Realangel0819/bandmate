package com.bandmate.band.dto;

import com.bandmate.band.entity.Position;
import lombok.Data;

@Data
public class CreateBandRequest {
    private String name;
    private String description;
}
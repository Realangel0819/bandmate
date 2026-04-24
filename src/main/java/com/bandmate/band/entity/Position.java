package com.bandmate.band.entity;

public enum Position {
    VOCAL("보컬"),
    DRUM("드럼"),
    GUITAR("기타"),
    BASS("베이스"),
    KEYBOARD("키보드"),
    PERCUSSION("퍼커션");

    private final String description;

    Position(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
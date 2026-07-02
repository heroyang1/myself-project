package com.backstage.system.enums.book;

public enum BookMediaTypeEnum {
    EBOOK(1),
    COMIC(2);

    private final Integer code;

    BookMediaTypeEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static Integer normalize(Integer code) {
        return COMIC.code.equals(code) ? COMIC.code : EBOOK.code;
    }

    public static boolean isComic(Integer code) {
        return COMIC.code.equals(normalize(code));
    }
}

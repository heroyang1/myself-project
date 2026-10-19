package com.backstage.common.enums;

public enum UploadPathEnum {

        AVATAR("common/image/avatar/"),

        COURSE_VIDEO("common/video/course/"),

        COURSE_COVER("common/image/courseCover/"),

        DOCUMENT("common/document/"),

        COURSE_MATERIAL("common/material/course/"),

       TOOL_COVER("common/image/toolCover/"),

        CONTENT_BACKGROUND("common/image/contentBackground/"),

        AUDIO("common/audio/"),

        IMAGE("common/image/"),

        TEMP("temp/"),

        INNER_SITE("site/"),

        RESOURCE("resource/"),

        DEFAULT("common/");


        private final String path;

        UploadPathEnum(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }




    public static UploadPathEnum fromPath(String path) {
        if (path == null || path.isEmpty()) {
            return DEFAULT;
        }
        for (UploadPathEnum uploadPath : values()) {
            if (uploadPath.path.equals(path)) {
                return uploadPath;
            }
        }
        return DEFAULT;
    }


        public static boolean isValidPath(String path) {
            if (path == null || path.isEmpty()) {
                return false;
            }
            for (UploadPathEnum uploadPath : values()) {
                if (uploadPath.path.equals(path)) {
                    return true;
                }
            }
            return false;
        }


}

package com.backstage.system.book;

import com.backstage.system.utils.UserContextUtil;
import org.springframework.stereotype.Component;

/**
 * SpEL helper for ebook management authorization.
 */
@Component("bookLevelAuth")
public class BookLevelAuthorization {

    public boolean canManageBook() {
        return UserContextUtil.getCurrentLevelSafely() >= 4;
    }

    public boolean canRestoreBook() {
        return UserContextUtil.getCurrentLevelSafely() == 6;
    }
}

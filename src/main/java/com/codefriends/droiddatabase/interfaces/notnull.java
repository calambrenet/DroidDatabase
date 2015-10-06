package com.codefriends.droiddatabase.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by calambrenet on 27/09/15.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface notnull {
    boolean value();
}

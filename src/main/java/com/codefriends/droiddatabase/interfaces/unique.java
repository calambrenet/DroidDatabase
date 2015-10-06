package com.codefriends.droiddatabase.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by calambrenet on 4/10/15.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface unique {
    boolean value();
}

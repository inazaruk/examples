package com.inazaruk.app.test;

import com.google.common.base.Preconditions;
import com.jayway.android.robotium.solo.Solo;

import junit.framework.TestCase;

public class Test extends TestCase {
    
    public void test() {
        Solo solo = new Solo(null);
        Preconditions.checkArgument(true);//verify guava from "app" is available .
        assertTrue(true);
    }

}

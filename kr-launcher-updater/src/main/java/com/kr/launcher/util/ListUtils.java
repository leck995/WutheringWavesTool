package com.kr.launcher.util;

import java.util.*;

public class ListUtils {

    public static <T> void shuffle(List<T> list) {
        Random rand = new Random();
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}

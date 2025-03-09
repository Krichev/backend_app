package com.my.challenger.lang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Endpoint {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UI {

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class V1 {

            private static final String VERSION = "/ui/api/v1";

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class User {

                public static final String ROOT = VERSION + "/user";
                public static final String GET_USER = "/by-name";
                public static final String ADD_NEW_USER = "/add-new";
            }
        }
    }
}
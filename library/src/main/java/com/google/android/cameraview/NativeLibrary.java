package com.google.android.cameraview;

public class NativeLibrary {

        static {
            System.loadLibrary("native-lib");
        }

        public static native void yuv420p2rgba(byte[] yuv420p,
                                               int width,
                                               int height,
                                               byte[] rgba);
}

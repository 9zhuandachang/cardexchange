g++  ^
-g Matcher_JNI.cpp ^
-o ..\dll\HM_CardMatcher.dll ^
-shared ^
-I "C:\Program Files\Java\jdk-19\include" ^
-I "C:\Program Files\Java\jdk-19\include\win32" ^
-I C:/opencv/build/x64/MinGW/install/include ^
-L C:/opencv/build/x64/MinGW/bin ^
-lopencv_calib3d4100 -lopencv_core4100 -lopencv_dnn4100 -lopencv_features2d4100 ^
-lopencv_flann4100 -lopencv_highgui4100 -lopencv_imgcodecs4100 -lopencv_imgproc4100 ^
-lopencv_ximgproc4100 -lopencv_ml4100 -lopencv_objdetect4100 -lopencv_photo4100 ^
-lopencv_stitching4100 -lopencv_video4100 -lopencv_videoio4100 ^
-std=c++17 ^
-Wl,-rpath,./dll

pause

# androidthings-cameraCar
An Android Things RobotCar with Camera

Android Things智能小车
-----

目前是0.1版本，这已经是一个可以运动、可以调速、可以遥控、可以避免碰撞的简单但完整的小车了。

![小车0.1版实物图](https://github.com/sysolve/androidthings-cameraCar/blob/master/photos/car_v0.1.png)

实现了超声波距离检测，当前方距离小于100mm时停车。

可以通过Wifi连接，内置了使用NanoHTTPD实现的服务器。打开手机或电脑浏览器，可以实现遥控。

![小车0.1版实物图](https://github.com/sysolve/androidthings-cameraCar/blob/master/photos/car_v0.1_control.png)

车上安装了一个触摸按钮，作为急停功能，按下时车轮电机会立即停转。

智能小车项目规划
----

1. 通过PWM实现车轮调速  **已实现**
2. 实现前进、后退、左转、右转等基本运动  **已实现**
3. 实现急停功能，用户可通过急停按钮停止小车  **已实现**
4. 超声波距离检测，离障碍物过近时自动停车  **已实现**
5. HTTP服务器，可通过手机/电脑遥控  **已实现**
6. 优化程序结构
7. 优化HTTP控制界面
8. 微信小程序控制
9. 加入摄像头实现拍照功能
10. 加入AI功能，对照片进行分析识别
11. 加入云台可调整摄像头角度


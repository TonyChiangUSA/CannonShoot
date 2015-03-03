游戏简介：
本游戏要求玩家在10秒内摧毁7个子块组成的标靶，整个游戏界面由大炮，挡板，和标靶组成，通过点触荧幕可以使大炮进行瞄准，双击发射炮弹，碰到挡板游戏时间减少，击中目标会增加游戏时间，当游戏时间减至0时结束游戏
工程结构：
这个应用由三个类组成：
1）Line：使两个点成为一个组，使用该类来定义挡板和靶子，blocker = new Line()，target = new Line();
2）CannonGame是应用的主Activity，主要用来处理双击事件（onTouchEvent(MotionEvent event)，和cannonView.alignCannon(event);）和主Activity的onCreate，onPause（调用 cannonView.stopGame()），onDestroy（ cannonView.releaseResources()）
3）CannonView是自己扩展自SurfaceView的子类实现了SurfaceHolder.Callback界面，该界面包含了三个事 件： surfaceCreated，surfaceChanged（该方法体为空，因为此应用中一直以纵向视图显示，不会调用该方法）和 surfaceDestroyed（调用cannonThread.join()）
->构造函数CannonView(Context context, AttributeSet attrs)用来初始化挡板，靶子和大炮，初始化声音等
->onSizeChanged(int w, int h, int oldw, int oldh)初次加载时调用，完成绘图，并调用newGame函数；
->newGame（）重置所有的界面并且启动的新的线程Cannon Thread来启动新游戏
-> updatePositions(double elapsedTimeMS)由Cannon Thread调用，由游戏运行的时间来跟新下一时刻个元素的位置，并执行简单的冲突检验，判断小球是否与挡板接触，如果冲突了，则小球方向变反，剩余时间 减少，并播放击中挡板的声音；如果，小球碰到了左右,上下边界则cannonballOnScreen=false;之后，进行小球和target的冲突 检验，如果碰到目标，判断击中了哪一块，剩余时间增加，cannonballOnScreen=false;如果击中的已经是最后一块了，游戏结束
-> alignCannon(MotionEvent event)将炮口对准手指按下的位置，并返回其与水平线的夹角
-> fireCannonball(MotionEvent event),由双击事件触发，计算炮弹的水平分量和垂直分量，shotsFired++，cannonballOnScreen=true;
->drawGameElements(Canvas canvas)绘制荧幕，有CannonThread调用，canvas是CannonThread从surfaceView的SurfaceHolder中获得的
->showGameOverDialog(int messageId),游戏结束时调用，为button设置点击事件调用newGame，因为对话框必须在GUI线程中显示，所以 activity.runOnUiThread
技术概览：
一.string.xml中的格式化资源 Shots fired: %1$d\nTotal time: %2$.1f
调用： dialogBuilder.setMessage(getResources().getString( R.string.results_format, shotsFired, totalElapsedTime));
二.界面布局一直是竖向的 android:screenOrientation="portrait"//界面布局一直是竖向的
三.将自定义的布局和布局（main.xml绑定） 1）需要使用完全限定类名：
四.使用soundpool和audiomanager播放声音： soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0); 第一个参数表示一次课播放的最大同步声音流数量，这里一次只播放一个声音，所以设置为1，第二个参数表示制定那种流进行播放，一共有7个，google推 荐在游戏中使用AudioManager.STREAM_MUSIC
播放： soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);
// allow volume keys to set game volume setVolumeControlStream(AudioManager.STREAM_MUSIC);
->MediaPlay和soundpool的区别： 使用MediaPlayer来播放音频档存在一些不足: 例如：资源占用量较高、延迟时间较长、不支持多个音频同时播放等。
相对于使用SoundPool存在的一些问题: 1. SoundPool最大只能申请1M的存储器空间，这就意味着我们只能使用一些很短的声音片段，而不是用它来播放歌曲或者游戏背景音乐（背景音乐可以考虑 使用JetPlayer来播放） 2. SoundPool提供了pause和stop方法，但这些方法建议最好不要轻易使用，因为有些时候它们可能会使你的程序莫名其妙的终止。还有些朋友反映 它们不会立即中止播放声音，而是把缓冲区里的数据播放完才会停下来，也许会多播放一秒钟。
五.Cannon Game类的Activity的生命周期OnPause和OnDestory
1）Cannon Game类是游戏的主Activity类 2）custom view to display the game：private CannonView cannonView; 2）重写OnPause和OnDestory分别调用cannonView.stopGame()和 cannonView.releaseResources();
六.重写Cannon Game类的Activity的onTouchEvent方法
//检查是否为双击事件 return gestureDetector.onTouchEvent(event);
七.利用GestureDetector和SimpleOnGestureListener处理双击事件
现在Cannon Game类的onCreate方法中初始化 GestureDetector gestureDetector = new GestureDetector(this, gestureListener);
八.CannonThread实现游戏循环（多线程同步）：
因为一次只能有一个线程访问surfaceview，所以要枷锁
2）surfaceDestroyed（调用cannonThread.join()）
在很多情况下，主线程生成并起动了子线程，如果子线程里要进行大量的耗时的运算，主线程往往将于子线程之前结束，但是如果主线程处理完其他的事务 后，需要用到子线程的处理结果，也就是主线程需要等待子线程执行完成之后再结束，这个时候就要用到join()方法了。即join()的作用是：“等待该 线程终止”，这里需要理解的就是该线程是指的主线程等待子线程的终止。也就是在子线程调用了join()方法后面的代码，只有等到子线程结束了才能执行。
九.简单的冲突检查
判断球是否碰触左右上下边界，以及挡板和目标
十.使用paint和canvas绘图
Tips：
1)
android:screenOrientation="portrait"//界面布局一直是竖向的
// initialize the GestureDetector，通过gestureListener来监听手势变化 gestureDetector = new GestureDetector(this, gestureListener);
// 允许设备音量键控制声音 setVolumeControlStream(AudioManager.STREAM_MUSIC);
4）
soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0); 第一个参数表示一次课播放的最大同步声音流数量，这里一次只播放一个声音，所以设置为1，第二个参数表示制定那种流进行播放，一共有7个，google推 荐在游戏中使用AudioManager.STREAM_MUSIC
5）
paint.setAntiAlias(ture)//抗锯齿，是图形平滑 cannonPaint.setStrokeWidth(lineWidth * 1.5f);//设置线宽

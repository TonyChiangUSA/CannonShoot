遊戲簡介：

本遊戲要求玩家在10秒內摧毀7個子塊組成的標靶，整個遊戲界面由大炮，擋板，和標靶組成，通過點觸螢幕可以使大炮進行瞄準，雙擊發射炮彈，碰到擋板遊戲時間減少，擊中目標會增加遊戲時間，當遊戲時間減至0時結束遊戲

工程結構：

這個應用由三個類組成：

1）Line：使兩個點成為一個組，使用該類來定義擋板和靶子，blocker = new Line()，target = new Line();

2）CannonGame是應用的主Activity，主要用來處理雙擊事件（onTouchEvent(MotionEvent event)，和cannonView.alignCannon(event);）和主Activity的onCreate，onPause（調用cannonView.stopGame()），onDestroy（ cannonView.releaseResources()）

3）CannonView是自己擴展自SurfaceView的子類實現了SurfaceHolder.Callback介面，該介面包含了三個事件：
 surfaceCreated，surfaceChanged（該方法體為空，因為此應用中一直以縱向視圖顯示，不會調用該方法）和surfaceDestroyed（調用cannonThread.join()）

->構造函數CannonView(Context context, AttributeSet attrs)用來初始化擋板，靶子和大炮，初始化聲音等

->onSizeChanged(int w, int h, int oldw, int oldh)初次加載時調用，完成繪圖，並調用newGame函數；

->newGame（）重置所有的界面並且啟動的新的線程Cannon Thread來啟動新遊戲

-> updatePositions(double elapsedTimeMS)由Cannon Thread調用，由遊戲運行的時間來跟新下一時刻個元素的位置，並執行簡單的衝突檢驗，判斷小球是否與擋板接觸，如果衝突了，則小球方向變反，剩餘時間減少，並播放擊中擋板的聲音；如果，小球碰到了左右,上下邊界則cannonballOnScreen=false;之後，進行小球和target的衝突檢驗，如果碰到目標，判斷擊中了哪一塊，剩餘時間增加，cannonballOnScreen=false;如果擊中的已經是最後一塊了，遊戲結束

-> alignCannon(MotionEvent event)將炮口對準手指按下的位置，並返回其與水平線的夾角

-> fireCannonball(MotionEvent event),由雙擊事件觸發，計算炮彈的水準分量和垂直分量，shotsFired++，cannonballOnScreen=true;

->drawGameElements(Canvas canvas)繪製螢幕，有CannonThread調用，canvas是CannonThread從surfaceView的SurfaceHolder中獲得的

->showGameOverDialog(int messageId),遊戲結束時調用，為button設置點擊事件調用newGame，因為對話框必須在GUI線程中顯示，所以
activity.runOnUiThread


技術概覽：

一.string.xml中的格式化資源
<string name="results_format"> Shots fired: %1$d\nTotal time: %2$.1f</string>

調用：
dialogBuilder.setMessage(getResources().getString( R.string.results_format, shotsFired, totalElapsedTime));


二.界面佈局一直是豎向的
android:screenOrientation="portrait"//介面佈局一直是豎向的


三.將自定義的佈局和佈局（main.xml綁定）
1）需要使用完全限定類名：
<com.tonychiang.cannonshoot.CannonView/>


四.使用soundpool和audiomanager播放聲音：
soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
第一個參數表示一次課播放的最大同步聲音流數量，這裏一次只播放一個聲音，所以設置為1，第二個參數表示制定那種流進行播放，一共有7個，google推薦在遊戲中使用AudioManager.STREAM_MUSIC


播放：
soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);

// allow volume keys to set game volume
setVolumeControlStream(AudioManager.STREAM_MUSIC);

->MediaPlay和soundpool的區別：
使用MediaPlayer來播放音頻檔存在一些不足:
例如：資源佔用量較高、延遲時間較長、不支持多個音頻同時播放等。

相對於使用SoundPool存在的一些問題:
1. SoundPool最大只能申請1M的記憶體空間，這就意味著我們只能使用一些很短的聲音片段，而不是用它來播放歌曲或者遊戲背景音樂（背景音樂可以考慮使用JetPlayer來播放）
2. SoundPool提供了pause和stop方法，但這些方法建議最好不要輕易使用，因為有些時候它們可能會使你的程式莫名其妙的終止。還有些朋友反映它們不會立即中止播放聲音，而是把緩衝區裏的數據播放完才會停下來，也許會多播放一秒鐘。 


五.Cannon Game類的Activity的生命週期OnPause和OnDestory

1）Cannon Game類是遊戲的主Activity類
2）custom view to display the game：private CannonView cannonView; 
2）重寫OnPause和OnDestory分別調用cannonView.stopGame()和cannonView.releaseResources();


六.重寫Cannon Game類的Activity的onTouchEvent方法

//檢查是否為雙擊事件
return gestureDetector.onTouchEvent(event);


七.利用GestureDetector和SimpleOnGestureListener處理雙擊事件

現在Cannon Game類的onCreate方法中初始化 GestureDetector
 gestureDetector = new GestureDetector(this, gestureListener);

八.CannonThread實現遊戲迴圈（多線程同步）：

因為一次只能有一個線程訪問surfaceview，所以要枷鎖


2）surfaceDestroyed（調用cannonThread.join()）

在很多情況下，主線程生成並起動了子線程，如果子線程裏要進行大量的耗時的運算，主線程往往將於子線程之前結束，但是如果主線程處理完其他的事務後，需要用到子線程的處理結果，也就是主線程需要等待子線程執行完成之後再結束，這個時候就要用到join()方法了。即join()的作用是：“等待該線程終止”，這裏需要理解的就是該線程是指的主線程等待子線程的終止。也就是在子線程調用了join()方法後面的代碼，只有等到子線程結束了才能執行。


九.簡單的衝突檢查

判斷球是否碰觸左右上下邊界，以及擋板和目標

十.使用paint和canvas繪圖



Tips：

1) 

android:screenOrientation="portrait"//介面佈局一直是豎向的

// initialize the GestureDetector，通過gestureListener來監聽手勢變化
 gestureDetector = new GestureDetector(this, gestureListener);

// 允許設備音量鍵控制聲音
 setVolumeControlStream(AudioManager.STREAM_MUSIC);

4）

soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
第一個參數表示一次課播放的最大同步聲音流數量，這裏一次只播放一個聲音，所以設置為1，第二個參數表示制定那種流進行播放，一共有7個，google推薦在遊戲中使用AudioManager.STREAM_MUSIC

5）

paint.setAntiAlias(ture)//抗鋸齒，是圖形平滑
cannonPaint.setStrokeWidth(lineWidth * 1.5f);//設置線寬

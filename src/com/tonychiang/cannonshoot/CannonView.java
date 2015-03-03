package com.tonychiang.cannonshoot;

import java.util.HashMap;
import java.util.Map;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CannonView extends SurfaceView 
   implements SurfaceHolder.Callback
{
   private CannonThread cannonThread; // controls the game loop
   private Activity activity; // to display Game Over dialog in GUI thread
   private boolean dialogIsDisplayed = false;   
               
   public static final int TARGET_PIECES = 7; 
   public static final int MISS_PENALTY = 2; 
   public static final int HIT_REWARD = 3; 

   private boolean gameOver; 
   private double timeLeft; 
   private int shotsFired; 
   private double totalElapsedTime; 

   private Line blocker; 
   private int blockerDistance; // blocker distance from left
   private int blockerBeginning; // blocker distance from top
   private int blockerEnd; // blocker bottom edge distance from top
   private int initialBlockerVelocity; 
   private float blockerVelocity;

   private Line target; 
   private int targetDistance; // target distance from left
   private int targetBeginning; // target distance from top
   private double pieceLength; 
   private int targetEnd; // target bottom's distance from top
   private int initialTargetVelocity; 
   private float targetVelocity; 

   private int lineWidth; 
   private boolean[] hitStates; 
   private int targetPiecesHit; 

   private Point cannonball; 
   private int cannonballVelocityX; 
   private int cannonballVelocityY; 
   private boolean cannonballOnScreen; 
   private int cannonballRadius; 
   private int cannonballSpeed; 
   private int cannonBaseRadius; 
   private int cannonLength; 
   private Point barrelEnd; 
   private int screenWidth; 
   private int screenHeight; 

   private static final int TARGET_SOUND_ID = 0;
   private static final int CANNON_SOUND_ID = 1;
   private static final int BLOCKER_SOUND_ID = 2;
   private SoundPool soundPool; 
   private Map<Integer, Integer> soundMap; 

   private Paint textPaint; 
   private Paint cannonballPaint; 
   private Paint cannonPaint; 
   private Paint blockerPaint; 
   private Paint targetPaint;
   private Paint backgroundPaint; 

   @SuppressLint("UseSparseArrays")
public CannonView(Context context, AttributeSet attrs)
   {
      super(context, attrs); 
      activity = (Activity) context; 
      
      // register SurfaceHolder.Callback listener
      getHolder().addCallback(this); 

      blocker = new Line(); 
      target = new Line(); 
      cannonball = new Point(); 

      hitStates = new boolean[TARGET_PIECES];

      soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

      soundMap = new HashMap<Integer, Integer>(); 
      soundMap.put(TARGET_SOUND_ID,
         soundPool.load(context, R.raw.target_hit, 1));
      soundMap.put(CANNON_SOUND_ID,
         soundPool.load(context, R.raw.cannon_fire, 1));
      soundMap.put(BLOCKER_SOUND_ID,
         soundPool.load(context, R.raw.blocker_hit, 1));

      // construct Paints for drawing text, cannonball, cannon,
      // blocker and target; these are configured in method onSizeChanged
      textPaint = new Paint(); 
      cannonPaint = new Paint(); 
      cannonballPaint = new Paint();
      blockerPaint = new Paint(); 
      targetPaint = new Paint();
      backgroundPaint = new Paint(); 
   } 

   @Override
   protected void onSizeChanged(int w, int h, int oldw, int oldh)
   {
      super.onSizeChanged(w, h, oldw, oldh);

      screenWidth = w; 
      screenHeight = h;
      cannonBaseRadius = h / 18; 
      cannonLength = w / 8; 

      cannonballRadius = w / 36; 
      cannonballSpeed = w * 3 / 2;

      lineWidth = w / 24; 

      blockerDistance = w * 5 / 8; 
      blockerBeginning = h / 8; 
      blockerEnd = h * 3 / 8; 
      initialBlockerVelocity = h / 2; 
      blocker.start = new Point(blockerDistance, blockerBeginning);
      blocker.end = new Point(blockerDistance, blockerEnd);

      targetDistance = w * 7 / 8;
      targetBeginning = h / 8;
      targetEnd = h * 7 / 8; 
      pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
      initialTargetVelocity = -h / 4; 
      target.start = new Point(targetDistance, targetBeginning);
      target.end = new Point(targetDistance, targetEnd);

      barrelEnd = new Point(cannonLength, h / 2);

      textPaint.setTextSize(w / 20); 
      textPaint.setAntiAlias(true); 
      cannonPaint.setStrokeWidth(lineWidth * 1.5f); 
      blockerPaint.setStrokeWidth(lineWidth);     
      targetPaint.setStrokeWidth(lineWidth);     
      backgroundPaint.setColor(Color.WHITE);

      newGame(); 
   } 

   // reset all the screen elements and start a new game
   public void newGame()
   {
      for (int i = 0; i < TARGET_PIECES; ++i)
         hitStates[i] = false;

      targetPiecesHit = 0; 
      blockerVelocity = initialBlockerVelocity; 
      targetVelocity = initialTargetVelocity; 
      timeLeft = 10; 
      cannonballOnScreen = false; 
      shotsFired = 0; 
      totalElapsedTime = 0.0; 
      blocker.start.set(blockerDistance, blockerBeginning);
      blocker.end.set(blockerDistance, blockerEnd);
      target.start.set(targetDistance, targetBeginning);
      target.end.set(targetDistance, targetEnd);
      
      if (gameOver)
      {
         gameOver = false; 
         cannonThread = new CannonThread(getHolder());
         cannonThread.start();
      } 
   } 

   // called repeatedly by the CannonThread to update game elements
   private void updatePositions(double elapsedTimeMS)
   {
      double interval = elapsedTimeMS / 1000.0;

      if (cannonballOnScreen) 
      {
         // update cannonball position
         cannonball.x += interval * cannonballVelocityX;
         cannonball.y += interval * cannonballVelocityY;

         // check for collision with blocker
         if (cannonball.x + cannonballRadius > blockerDistance && 
            cannonball.x - cannonballRadius < blockerDistance &&
            cannonball.y + cannonballRadius > blocker.start.y &&
            cannonball.y - cannonballRadius < blocker.end.y)
         {
            cannonballVelocityX *= -1; 
            timeLeft -= MISS_PENALTY; 

            
            soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);
         } 

         // check for collisions with left and right walls
         else if (cannonball.x + cannonballRadius > screenWidth || 
            cannonball.x - cannonballRadius < 0)
            cannonballOnScreen = false; 

         // check for collisions with top and bottom walls
         else if (cannonball.y + cannonballRadius > screenHeight || 
            cannonball.y - cannonballRadius < 0)
            cannonballOnScreen = false;

         // check for cannonball collision with target
         else if (cannonball.x + cannonballRadius > targetDistance && 
            cannonball.x - cannonballRadius < targetDistance && 
            cannonball.y + cannonballRadius > target.start.y &&
            cannonball.y - cannonballRadius < target.end.y)
         {
            // determine target section number (0 is the top)
            int section = 
               (int) ((cannonball.y - target.start.y) / pieceLength);
            
            // check if the piece hasn't been hit yet
            if ((section >= 0 && section < TARGET_PIECES) && 
               !hitStates[section])
            {
               hitStates[section] = true; 
               cannonballOnScreen = false; 
               timeLeft += HIT_REWARD; 
               
               soundPool.play(soundMap.get(TARGET_SOUND_ID), 1,
                  1, 1, 0, 1f);

               // if all pieces have been hit
               if (++targetPiecesHit == TARGET_PIECES)
               {
                  cannonThread.setRunning(false);
                  showGameOverDialog(R.string.win);
                  gameOver = true; 
               } 
            } 
         }
      } 

      // update the blocker's position
      double blockerUpdate = interval * blockerVelocity;
      blocker.start.y += blockerUpdate;
      blocker.end.y += blockerUpdate;

      // update the target's position
      double targetUpdate = interval * targetVelocity;
      target.start.y += targetUpdate;
      target.end.y += targetUpdate;

      // if the blocker hit the top or bottom, reverse direction
      if (blocker.start.y < 0 || blocker.end.y > screenHeight)
         blockerVelocity *= -1;

      // if the target hit the top or bottom, reverse direction
      if (target.start.y < 0 || target.end.y > screenHeight)
         targetVelocity *= -1;

      timeLeft -= interval; 

      
      if (timeLeft <= 0.0)
      {
         timeLeft = 0.0;
         gameOver = true; 
         cannonThread.setRunning(false);
         showGameOverDialog(R.string.lose);
      } 
   } 

   // fires a cannonball
   public void fireCannonball(MotionEvent event)
   {
      if (cannonballOnScreen) 
         return; 

      double angle = alignCannon(event); // get the cannon barrel's angle

      // move the cannonball to be inside the cannon
      cannonball.x = cannonballRadius; // align x-coordinate with cannon
      cannonball.y = screenHeight / 2; // centers ball vertically

      // get the x component of the total velocity
      cannonballVelocityX = (int) (cannonballSpeed * Math.sin(angle));

      // get the y component of the total velocity
      cannonballVelocityY = (int) (-cannonballSpeed * Math.cos(angle));
      cannonballOnScreen = true; // the cannonball is on the screen
      ++shotsFired; // increment shotsFired

      // play cannon fired sound
      soundPool.play(soundMap.get(CANNON_SOUND_ID), 1, 1, 1, 0, 1f);
   } // end method fireCannonball

   // aligns the cannon in response to a user touch
   public double alignCannon(MotionEvent event)
   {
      Point touchPoint = new Point((int) event.getX(), (int) event.getY());

      double centerMinusY = (screenHeight / 2 - touchPoint.y);

      double angle = 0; 

      if (centerMinusY != 0) // prevent division by 0
         angle = Math.atan((double) touchPoint.x / centerMinusY);

      // if the touch is on the lower half of the screen
      if (touchPoint.y > screenHeight / 2)
         angle += Math.PI; 
      
      // calculate the endpoint of the cannon barrel
      barrelEnd.x = (int) (cannonLength * Math.sin(angle));
      barrelEnd.y = 
         (int) (-cannonLength * Math.cos(angle) + screenHeight / 2);

      return angle; 
   } 

   // draws the game to the given Canvas
   public void drawGameElements(Canvas canvas)
   {
      // clear the background
      canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), 
         backgroundPaint);
      
      // display time remaining
      canvas.drawText(getResources().getString(
         R.string.time_remaining_format, timeLeft), 30, 50, textPaint);

      if (cannonballOnScreen)
         canvas.drawCircle(cannonball.x, cannonball.y, cannonballRadius,
            cannonballPaint);

      canvas.drawLine(0, screenHeight / 2, barrelEnd.x, barrelEnd.y,
         cannonPaint);

      canvas.drawCircle(0, (int) screenHeight / 2,
         (int) cannonBaseRadius, cannonPaint);

      canvas.drawLine(blocker.start.x, blocker.start.y, blocker.end.x,
         blocker.end.y, blockerPaint);

      Point currentPoint = new Point(); 

      currentPoint.x = target.start.x;
      currentPoint.y = target.start.y;

      for (int i = 1; i <= TARGET_PIECES; ++i)
      {
         if (!hitStates[i - 1])
         {
            if (i % 2 == 0)
               targetPaint.setColor(Color.RED);
            else
               targetPaint.setColor(Color.GREEN);
            
            canvas.drawLine(currentPoint.x, currentPoint.y, target.end.x,
               (int) (currentPoint.y + pieceLength), targetPaint);
         } 
         
         // move curPoint to the start of the next piece
         currentPoint.y += pieceLength;
      } 
   } 

   private void showGameOverDialog(int messageId)
   {
      final AlertDialog.Builder dialogBuilder = 
         new AlertDialog.Builder(getContext());
      dialogBuilder.setTitle(getResources().getString(messageId));
      dialogBuilder.setCancelable(false);

      dialogBuilder.setMessage(getResources().getString(
         R.string.results_format, shotsFired, totalElapsedTime));
      dialogBuilder.setPositiveButton(R.string.reset_game,
         new DialogInterface.OnClickListener()
         {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
               dialogIsDisplayed = false;
               newGame(); 
            } 
         } 
      );

      activity.runOnUiThread(
         new Runnable() {
            public void run()
            {
               dialogIsDisplayed = true;
               dialogBuilder.show(); 
            }
         } 
      ); 
   } 

  
   public void stopGame()
   {
      if (cannonThread != null)
         cannonThread.setRunning(false);
   } 

   // releases resources; called by CannonGame's onDestroy method 
   public void releaseResources()
   {
      soundPool.release(); 
      soundPool = null; 
   } 

   // called when surface changes size
   @Override
   public void surfaceChanged(SurfaceHolder holder, int format,
      int width, int height)
   {
   }
   
   // called when surface is first created
   @Override
   public void surfaceCreated(SurfaceHolder holder)
   {
      if (!dialogIsDisplayed)
      {
         cannonThread = new CannonThread(holder);
         cannonThread.setRunning(true);
         cannonThread.start(); 
      } 
   } 

   // called when the surface is destroyed
   @Override
   public void surfaceDestroyed(SurfaceHolder holder)
   {
      
      boolean retry = true;
      cannonThread.setRunning(false);
      
      while (retry)
      {
         try
         {
            cannonThread.join();
            retry = false;
         } 
         catch (InterruptedException e)
         {
         } 
      } 
   } 
   
   // Thread subclass to control the game loop
   private class CannonThread extends Thread
   {
      private SurfaceHolder surfaceHolder; // for manipulating canvas
      private boolean threadIsRunning = true; 
      
      public CannonThread(SurfaceHolder holder)
      {
         surfaceHolder = holder;
         setName("CannonThread");
      } 
      
      public void setRunning(boolean running)
      {
         threadIsRunning = running;
      } 
      
      // controls the game loop
      @Override
      public void run()
      {
         Canvas canvas = null; 
         long previousFrameTime = System.currentTimeMillis(); 
        
         while (threadIsRunning)
         {
            try
            {
               canvas = surfaceHolder.lockCanvas(null);               
               
               // lock the surfaceHolder for drawing
               synchronized(surfaceHolder)
               {
                  long currentTime = System.currentTimeMillis();
                  double elapsedTimeMS = currentTime - previousFrameTime;
                  totalElapsedTime += elapsedTimeMS / 1000.00; 
                  updatePositions(elapsedTimeMS); 
                  drawGameElements(canvas); 
                  previousFrameTime = currentTime; 
               } 
            } 
            finally
            {
               if (canvas != null) 
                  surfaceHolder.unlockCanvasAndPost(canvas);
            } 
         } 
      }
   } 
} 


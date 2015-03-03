package com.tonychiang.cannonshoot;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import com.deitel.cannongame.R;


public class CannonGame extends Activity
{
   private GestureDetector gestureDetector; // listens for double taps
   private CannonView cannonView; 

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState); 
      setContentView(R.layout.main); 

      cannonView = (CannonView) findViewById(R.id.cannonView);

      gestureDetector = new GestureDetector(this, gestureListener);
      
      // allow volume keys to set game volume
      setVolumeControlStream(AudioManager.STREAM_MUSIC);
   } 

   @Override
   public void onPause()
   {
      super.onPause(); 
      cannonView.stopGame(); 
   } 

   
   @Override
   protected void onDestroy()
   {
      super.onDestroy();
      cannonView.releaseResources();
   } 

  
   @Override
   public boolean onTouchEvent(MotionEvent event)
   {
      int action = event.getAction();

      if (action == MotionEvent.ACTION_DOWN ||
         action == MotionEvent.ACTION_MOVE)
      {
         cannonView.alignCannon(event); 
      } 

      return gestureDetector.onTouchEvent(event);
   } 

   SimpleOnGestureListener gestureListener = new SimpleOnGestureListener()
   {
      @Override
      public boolean onDoubleTap(MotionEvent e)
      {
         cannonView.fireCannonball(e);
         return true; 
      } 
   }; 
} 


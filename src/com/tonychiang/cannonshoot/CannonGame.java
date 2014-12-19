// CannonGame.java
// Main Activity for the Cannon Game app.
package com.tonychiang.cannonshoot;

import com.deitel.cannongame.R;


import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class CannonGame extends Activity
{
   private GestureDetector gestureDetector; // listens for double taps
   private CannonView cannonView; // custom view to display the game

   // called when the app first launches
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState); // call super's onCreate method
      setContentView(R.layout.main); // inflate the layout

      // get the CannonView
      cannonView = (CannonView) findViewById(R.id.cannonView);

      // initialize the GestureDetector
      gestureDetector = new GestureDetector(this, gestureListener);
      
      // allow volume keys to set game volume
      setVolumeControlStream(AudioManager.STREAM_MUSIC);
   } // end method onCreate

   // when the app is pushed to the background, pause it
   @Override
   public void onPause()
   {
      super.onPause(); // call the super method
      cannonView.stopGame(); // terminates the game
   } // end method onPause

   // release resources
   @Override
   protected void onDestroy()
   {
      super.onDestroy();
      cannonView.releaseResources();
   } // end method onDestroy

   // called when the user touches the screen in this Activity
   @Override
   public boolean onTouchEvent(MotionEvent event)
   {
      // get int representing the type of action which caused this event
      int action = event.getAction();

      // the user user touched the screen or dragged along the screen
      if (action == MotionEvent.ACTION_DOWN ||
         action == MotionEvent.ACTION_MOVE)
      {
         cannonView.alignCannon(event); // align the cannon
      } // end if

      // call the GestureDetector's onTouchEvent method
      return gestureDetector.onTouchEvent(event);
   } // end method onTouchEvent

   // listens for touch events sent to the GestureDetector
   SimpleOnGestureListener gestureListener = new SimpleOnGestureListener()
   {
      // called when the user double taps the screen
      @Override
      public boolean onDoubleTap(MotionEvent e)
      {
         cannonView.fireCannonball(e); // fire the cannonball
         return true; // the event was handled
      } // end method onDoubleTap
   }; // end gestureListener
} // end class CannonGame


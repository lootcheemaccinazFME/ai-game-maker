package com.flymaccin.mutantmidnight

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.*
import kotlin.math.*
import kotlin.random.Random

/** Native Android game loop: touch, keyboard/mouse, Bluetooth/USB gamepads. */
class MutantGameView(context: Context) : View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG); private val p=Player(); private val enemies=mutableListOf<Enemy>(); private val shots=mutableListOf<Shot>()
    private var w=1f; private var h=1f; private var score=0; private var wave=1; private var next=0f; private var last=SystemClock.elapsedRealtime(); private var aimX=0f; private var aimY=0f; private var dead=false
    private var moveX=0f; private var moveY=0f; private var stickX=0f; private var stickY=0f; private var rightX=0f; private var rightY=0f; private var firing=false
    private val key = BooleanArray(300)
    init { isFocusableInTouchMode=true; requestFocus(); aimX=500f; aimY=250f }
    data class Player(var x:Float=500f,var y:Float=280f,var hp:Float=100f,var cool:Float=0f)
    data class Enemy(var x:Float,var y:Float,var hp:Int,var speed:Float,val radius:Float,val hue:Int)
    data class Shot(var x:Float,var y:Float,var vx:Float,var vy:Float,var life:Float=1.1f)
    override fun onSizeChanged(width:Int,height:Int,ow:Int,oh:Int){w=width.toFloat();h=height.toFloat();if(p.x>width||p.y>height){p.x=w/2;p.y=h/2};aimX=p.x+100;aimY=p.y}
    override fun onDraw(c:Canvas){super.onDraw(c); val now=SystemClock.elapsedRealtime(); val dt=min(.035f,(now-last)/1000f);last=now;if(!dead) update(dt);draw(c);postInvalidateOnAnimation()}
    private fun update(dt:Float){
        var dx=moveX+stickX;var dy=moveY+stickY; val len=hypot(dx,dy).coerceAtLeast(1f);p.x=(p.x+dx/len*260*dt).coerceIn(18f,w-18);p.y=(p.y+dy/len*260*dt).coerceIn(18f,h-18)
        if(abs(rightX)>.18f||abs(rightY)>.18f){aimX=p.x+rightX*120;aimY=p.y+rightY*120};p.cool-=dt;if(firing)fire();next-=dt;if(next<=0){val count=5+wave*3;repeat(count){i->postDelayed({spawn()},(i*150).toLong())};next=count*.15f+4;wave++}
        shots.forEach{it.x+=it.vx*dt;it.y+=it.vy*dt;it.life-=dt};shots.removeAll{it.life<=0||it.x<0||it.x>w||it.y<0||it.y>h}
        enemies.forEach{e->val a=atan2(p.y-e.y,p.x-e.x);e.x+=cos(a)*e.speed*dt;e.y+=sin(a)*e.speed*dt;if(hypot(e.x-p.x,e.y-p.y)<e.radius+12)p.hp-=15*dt}
        shots.forEach{s->enemies.forEach{e->if(e.hp>0&&hypot(s.x-e.x,s.y-e.y)<e.radius+5){e.hp--;s.life=0f;if(e.hp<=0)score+=100}}};enemies.removeAll{it.hp<=0};if(p.hp<=0){p.hp=0f;dead=true}
    }
    private fun spawn(){if(dead)return;val a=Random.nextFloat()*6.283f;val d=max(w,h)*.65f;enemies+=Enemy(p.x+cos(a)*d,p.y+sin(a)*d,2+(wave/3),40+Random.nextFloat()*20+wave*3,13+Random.nextFloat()*8,if(wave%2==0)300 else 115)}
    private fun fire(){if(p.cool>0)return;p.cool=.18f;val a=atan2(aimY-p.y,aimX-p.x);shots+=Shot(p.x,p.y,cos(a)*680,sin(a)*680)}
    private fun draw(c:Canvas){c.drawColor(Color.rgb(16,23,53));paint.strokeWidth=2f;paint.color=Color.rgb(28,57,78);for(i in 0..w.toInt() step 40)c.drawLine(i.toFloat(),0f,i.toFloat(),h,paint);for(i in 0..h.toInt() step 40)c.drawLine(0f,i.toFloat(),w,i.toFloat(),paint)
        shots.forEach{paint.color=Color.YELLOW;c.drawRect(it.x-3,it.y-3,it.x+3,it.y+3,paint)};enemies.forEach{e->paint.color=Color.HSVToColor(floatArrayOf(e.hue.toFloat(),.8f,.9f));c.drawCircle(e.x,e.y,e.radius,paint);paint.color=Color.rgb(35,16,45);c.drawRect(e.x-e.radius*.5f,e.y-3,e.x-e.radius*.2f,e.y+2,paint);c.drawRect(e.x+e.radius*.2f,e.y-3,e.x+e.radius*.5f,e.y+2,paint)}
        val a=atan2(aimY-p.y,aimX-p.x);c.save();c.rotate(a*180/Math.PI.toFloat(),p.x,p.y);paint.color=Color.rgb(142,255,211);c.drawRect(p.x-11,p.y-11,p.x+11,p.y+11,paint);paint.color=Color.rgb(255,218,97);c.drawRect(p.x+7,p.y-3,p.x+30,p.y+3,paint);c.restore();paint.textSize=24f;paint.typeface=Typeface.MONOSPACE;paint.color=Color.rgb(255,218,97);c.drawText("HP ${p.hp.toInt()}    WAVE ${max(1,wave-1)}    SCORE ${score.toString().padStart(6,'0')}",20f,32f,paint);paint.color=Color.rgb(142,255,211);paint.textSize=16f;c.drawText("BLUETOOTH CONTROLLER READY",20f,h-20,paint)
        if(dead){paint.color=0xA8000000.toInt();c.drawRect(0f,0f,w,h,paint);paint.color=Color.rgb(255,77,170);paint.textSize=42f;c.drawText("GAME OVER — TAP TO RESTART",w*.22f,h*.5f,paint)}
    }
    override fun onTouchEvent(e:MotionEvent):Boolean { val x=e.x;val y=e.y;if(dead&&e.action==MotionEvent.ACTION_DOWN){reset();return true};when(e.action){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{if(x<w*.42f&&y>h*.58f){moveX=(x-w*.21f)/(w*.21f);moveY=(y-h*.79f)/(h*.21f)}else{aimX=x;aimY=y;firing=true}};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{moveX=0f;moveY=0f;firing=false}};return true }
    override fun onKeyDown(code:Int,e:KeyEvent):Boolean {if(code in key.indices)key[code]=true;when(code){KeyEvent.KEYCODE_W,KeyEvent.KEYCODE_DPAD_UP->moveY=-1f;KeyEvent.KEYCODE_S,KeyEvent.KEYCODE_DPAD_DOWN->moveY=1f;KeyEvent.KEYCODE_A,KeyEvent.KEYCODE_DPAD_LEFT->moveX=-1f;KeyEvent.KEYCODE_D,KeyEvent.KEYCODE_DPAD_RIGHT->moveX=1f;KeyEvent.KEYCODE_SPACE,KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_BUTTON_R1,KeyEvent.KEYCODE_BUTTON_R2->firing=true};return true}
    override fun onKeyUp(code:Int,e:KeyEvent):Boolean {when(code){KeyEvent.KEYCODE_W,KeyEvent.KEYCODE_S,KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN->moveY=0f;KeyEvent.KEYCODE_A,KeyEvent.KEYCODE_D,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT->moveX=0f;KeyEvent.KEYCODE_SPACE,KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_BUTTON_R1,KeyEvent.KEYCODE_BUTTON_R2->firing=false};return true}
    override fun onGenericMotionEvent(e:MotionEvent):Boolean {if(e.isFromSource(InputDevice.SOURCE_JOYSTICK)){stickX=axis(e,MotionEvent.AXIS_X);stickY=axis(e,MotionEvent.AXIS_Y);rightX=axis(e,MotionEvent.AXIS_Z);rightY=axis(e,MotionEvent.AXIS_RZ);return true};if(e.isFromSource(InputDevice.SOURCE_MOUSE)){aimX=e.x;aimY=e.y;return true};return super.onGenericMotionEvent(e)}
    private fun axis(e:MotionEvent,a:Int):Float=e.getAxisValue(a).let{if(abs(it)<.18f)0f else it}
    private fun reset(){enemies.clear();shots.clear();score=0;wave=1;next=0f;p.x=w/2;p.y=h/2;p.hp=100f;dead=false}
}

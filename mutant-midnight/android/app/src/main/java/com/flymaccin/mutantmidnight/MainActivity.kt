package com.flymaccin.mutantmidnight

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager

class MainActivity : Activity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); setContentView(MutantGameView(this)) }
}

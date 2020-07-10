package AvoidExtraneousGraphicsAndAnimations;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.Toast;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import com.sun.tools.javac.util.Name;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Piasy{github.com/Piasy} on 07/07/2017.
 */



public class MainActivity {

    private GLSurfaceView mGLSurfaceView;
    private DemoRenderer mRenderer;

    protected void onCreate(Bundle savedInstanceState) {
        //setContentView(R.layout.activity_main);

        //if (!Utils.supportGlEs20(this)) {
        //Toast.makeText(this, "GLES 2.0 not supported!", Toast.LENGTH_LONG).show();
        //finish();
        //return;
        //}

        //mGLSurfaceView = (GLSurfaceView) findViewById(R.id.surface);

        mGLSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new DemoRenderer(this);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    //@Override
    //protected void onDestroy() {
    //super.onDestroy();
    //}

    //@Override
    //protected void onPause() {
    //super.onPause();
    //mGLSurfaceView.onPause();
    //}

    //@Override
    //protected void onResume() {
    //super.onResume();
    //}
}
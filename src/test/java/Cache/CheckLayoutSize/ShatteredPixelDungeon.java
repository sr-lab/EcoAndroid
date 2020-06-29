package Cache.CheckLayoutSize;

/*
    import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
    import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
    import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
    import com.shatteredpixel.shatteredpixeldungeon.scenes.WelcomeScene;
    import com.watabou.noosa.Game;
    import com.watabou.noosa.RenderedText;
    import com.watabou.noosa.audio.Music;
    import com.watabou.noosa.audio.Sample;
    import com.watabou.utils.GameMath;
    import com.shatteredpixel.shatteredpixeldungeon.Assets;
    import javax.microedition.khronos.opengles.GL10;
    import java.util.Locale;
*/

import android.view.SurfaceView;

class Game { }
class PixelScene {
    public static final float MIN_WIDTH_L = 0;
    public static final float MIN_WIDTH_P = 0;
    public static final float MIN_HEIGHT_L = 0;
    public static final float MIN_HEIGHT_P = 0;
}

public class ShatteredPixelDungeon extends Game {
    private SurfaceView view;
    private int width;
    private int height;

/*
    public ShatteredPixelDungeon() {
        super( WelcomeScene.class );
    }
 */

    private void updateDisplaySize() {

        float dispWidth = view.getMeasuredWidth();
        float dispHeight = view.getMeasuredHeight();
        float dispRatio = dispWidth / (float) dispHeight;

        float renderWidth = dispRatio > 1 ? PixelScene.MIN_WIDTH_L : PixelScene.MIN_WIDTH_P;
        float renderHeight = dispRatio > 1 ? PixelScene.MIN_HEIGHT_L : PixelScene.MIN_HEIGHT_P;

        //force power saver in this case as all devices must run at at least 2x scale.
        if (dispWidth < renderWidth * 2 || dispHeight < renderHeight * 2) {
            // Preferences.INSTANCE.put(Preferences.KEY_POWER_SAVER, true);
        }
        if (powerSaver())
        {

            int maxZoom = (int) Math.min(dispWidth / renderWidth, dispHeight / renderHeight);

            renderWidth *= Math.max(2, Math.round(1f + maxZoom * 0.4f));
            renderHeight *= Math.max(2, Math.round(1f + maxZoom * 0.4f));

            if (dispRatio > renderWidth / renderHeight)
            {
                renderWidth = renderHeight * dispRatio;
            } else
            {
                renderHeight = renderWidth / dispRatio;
            }

            final int finalW = Math.round(renderWidth);
            final int finalH = Math.round(renderHeight);
            if (finalW != width || finalH != height)
            {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.getHolder().setFixedSize(finalW, finalH);
                    }
                });

            }
        } else
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.getHolder().setSizeFromLayout();
                }
            });
        }
    }

    private void runOnUiThread(Runnable runnable) {
    }

    public static boolean powerSaver(){
    //  return Preferences.INSTANCE.getBoolean( Preferences.KEY_POWER_SAVER, false );
        return false;
    }
}

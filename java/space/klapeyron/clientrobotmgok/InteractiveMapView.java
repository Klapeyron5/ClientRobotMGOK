package space.klapeyron.clientrobotmgok;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;



public class InteractiveMapView extends View {

    private MainActivity mainActivity;
    public int startX;
    public int startY;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint paint, mBitmapPaint;
    private final int horizontalCountOfCells;
    private final int verticalCountOfCells;
    private final int textCoordinatesSize = 14;

    private int screenWidth;
    private int screenHeight;
    private int cellWidth;
    private int cellHeight;
    private int cellsLineWidth = 2;

    private final int COLOR_RED = 0xffff0505;
    private final int COLOR_BLACK = 0xff000000;
    private final int COLOR_DARK_NAVY = 0xff140F5C;
    private final int COLOR_LIGHT_BLUE = 0xff0089FF;
    private final int COLOR_NAVY = 0xff281DC7;
    private final int COLOR_GRAY = 0xff8AA18D;

    int[][] landscape = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,1,1,1},
            {1,1,1,0,0,0,0,0,0,0,1,1,1},
            {1,1,1,0,0,0,0,0,0,0,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1}}; //(y,x)

    public InteractiveMapView(Context context) {
        super(context);
        mainActivity = (MainActivity) context;
        startX = mainActivity.currentX;
        startY = mainActivity.currentY;

        Log.i(MainActivity.TAG, "landscape strings: "+landscape.length);
        Log.i(MainActivity.TAG, "landscape columns: "+landscape[0].length);
        horizontalCountOfCells = landscape[1].length;
        verticalCountOfCells = landscape.length;

        Log.i(MainActivity.TAG, "Square: "+landscape[1][2]);
        Log.i(MainActivity.TAG, "Square: "+landscape[1][3]);

        Log.i(mainActivity.TAG, "1");
    //    setEmptyMapBitmap();
    //    drawRobotOnMap(mainActivity.currentX,mainActivity.currentY);
        drawConstructor(mainActivity.currentX,mainActivity.currentY);
        Log.i(mainActivity.TAG, "2");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i(mainActivity.TAG, "onDraw");
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }


    private int startnX;
    private int startnY;
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setEmptyMapBitmap();
                drawRobotOnMap(mainActivity.currentX, mainActivity.currentY);
                Paint paintForCoordinates = new Paint();
                paintForCoordinates.setColor(COLOR_DARK_NAVY);
                paintForCoordinates.setStrokeWidth(cellWidth-cellsLineWidth);

                float X = e.getX();
                float Y = e.getY();

                int nX = ((int)e.getX())/cellWidth;
                int nY = ((int)e.getY())/cellHeight;
                startnX = nX;
                startnY = nY;

                if ((nX<horizontalCountOfCells)&&(nY<verticalCountOfCells)&&(landscape[nY][nX]==0))
                    mCanvas.drawLine(nX * cellWidth + cellWidth / 2, nY * cellHeight + cellsLineWidth / 2,
                        nX * cellWidth + cellWidth / 2, nY * cellHeight + cellsLineWidth / 2 + cellHeight - cellsLineWidth, paintForCoordinates);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                setEmptyMapBitmap();
                drawRobotOnMap(mainActivity.currentX, mainActivity.currentY);
                paintForCoordinates = new Paint();
                paintForCoordinates.setColor(COLOR_DARK_NAVY);
                paintForCoordinates.setStrokeWidth(cellWidth-cellsLineWidth);

                X = e.getX();
                Y = e.getY();

                nX = ((int)e.getX())/cellWidth;
                nY = ((int)e.getY())/cellHeight;
                if ((nX==startnX)&&(nY==startnY)&&(nX<horizontalCountOfCells)&&(nY<verticalCountOfCells)&&(landscape[nY][nX]==0))
                    mCanvas.drawLine(nX * cellWidth + cellWidth / 2, nY * cellHeight + cellsLineWidth / 2,
                            nX * cellWidth + cellWidth / 2, nY * cellHeight + cellsLineWidth / 2 + cellHeight - cellsLineWidth, paintForCoordinates);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                setEmptyMapBitmap();
                drawRobotOnMap(mainActivity.currentX,mainActivity.currentY);
                paintForCoordinates = new Paint();
                paintForCoordinates.setColor(COLOR_NAVY);
                paintForCoordinates.setStrokeWidth(cellWidth - cellsLineWidth);

                X = e.getX();
                Y = e.getY();

                nX = ((int)e.getX())/cellWidth;
                nY = ((int)e.getY())/cellHeight;

                boolean flag = false;
                if ((nX==startnX)&&(nY==startnY)&&(nX<horizontalCountOfCells)&&(nY<verticalCountOfCells)&&(landscape[nY][nX]==0)) {
                    mCanvas.drawLine(nX * cellWidth + cellWidth / 2, nY * cellHeight + cellsLineWidth / 2,
                            nX * cellWidth + cellWidth / 2, nY * cellHeight + cellsLineWidth / 2 + cellHeight - cellsLineWidth, paintForCoordinates);
                    flag = true;
                }
                invalidate();

                //TODO //диалог с подтверждением отправки
                if ((flag)&&(mainActivity.clientState == MainActivity.CLIENT_ROBOT_READY))
                    openCallRobotDialog(nX,nY);
                break;
        }
        return true;
    }

    public void drawConstructor(int X, int Y) {
        setEmptyMapBitmap();
        drawRobotOnMap(X, Y);
        drawAbsolutePath();
        invalidate();
    }

    /**
     * Draw empty map on InteractiveMapView.mBitmap variable
     */
    private void setEmptyMapBitmap() {
        //вычисляем размеры экрана
        DisplayMetrics metrics = new DisplayMetrics();
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
        //TODO//вычисляем какая сторона меньше и подгоняем под нее размеры квадратов
        //сейчас всегда держим вертикальную ориентацию
        cellWidth = (screenWidth-15)/horizontalCountOfCells;
        cellHeight = cellWidth;

        mBitmap = Bitmap.createBitmap(screenWidth,screenHeight,Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        //определяем параметры кисти, которой будем рисовать сетку
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(0xffff0505); //0xAA-HTML, AA - прозрачность
        paint.setStrokeWidth(cellsLineWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        //кисть для координат на сетке
        Paint paintForCoordinates = new Paint();
        paintForCoordinates.setColor(0xff0022FF);
        paintForCoordinates.setStrokeWidth(1);
        paintForCoordinates.setTextSize(textCoordinatesSize);

        //рисуем сетку и координаты
        for(int x=0;x< horizontalCountOfCells+1;x++) {
            mCanvas.drawLine(x * cellWidth, 0, x * cellWidth, verticalCountOfCells * cellHeight, paint);
        }
        for(int x=0;x< horizontalCountOfCells;x++) {
            mCanvas.drawText(Integer.toString(x), x * cellWidth, verticalCountOfCells * cellHeight + textCoordinatesSize, paintForCoordinates);
        }
        for(int y=0;y< verticalCountOfCells+1;y++) {
            mCanvas.drawLine(0, y * cellHeight, horizontalCountOfCells * cellWidth, y * cellHeight, paint);
        }
        for(int y=0;y< verticalCountOfCells;y++) {
            mCanvas.drawText(Integer.toString(y), horizontalCountOfCells * cellWidth, y * cellHeight + textCoordinatesSize, paintForCoordinates);
        }

        for(int i=0;i<verticalCountOfCells;i++) {
            for(int j=0;j<horizontalCountOfCells;j++) {
                if (landscape[i][j] == 1)
                    drawSquare(j,i,COLOR_GRAY);
            }
        }
    }

    public void drawRobotOnMap(int X, int Y) {
        Paint paintR1 = new Paint();
        paintR1.setAntiAlias(true);
        paintR1.setDither(true);
        paintR1.setColor(COLOR_RED);
        paintR1.setStrokeWidth(cellsLineWidth);
        paintR1.setStyle(Paint.Style.FILL);

        Paint paintR2 = new Paint();
        paintR2.setAntiAlias(true);
        paintR2.setDither(true);
        paintR2.setColor(COLOR_BLACK);
        paintR2.setStrokeWidth(cellsLineWidth);
        paintR2.setStyle(Paint.Style.FILL);

        Paint paintR3 = new Paint();
        paintR3.setAntiAlias(true);
        paintR3.setDither(true);
        paintR3.setColor(COLOR_RED);
        paintR3.setStrokeWidth(cellsLineWidth);
        paintR3.setStyle(Paint.Style.STROKE);
        if ((X >= 0)&&(Y >= 0)) {
            mCanvas.drawCircle(X * cellWidth + cellWidth / 2, Y * cellHeight + cellHeight / 2, cellWidth / 4, paintR1);
            mCanvas.drawCircle(X * cellWidth + cellWidth / 2, Y * cellHeight + cellHeight / 2, cellWidth / 4 - 3, paintR2);
            mCanvas.drawText("R", X * cellWidth + cellWidth / 2, Y * cellHeight + cellHeight / 2, paintR3);
        }
    }

    private void drawSquare(int nX, int nY, int Color) {
        Paint paint = new Paint();
        paint.setColor(Color);
        paint.setStrokeWidth(cellWidth - cellsLineWidth);

        mCanvas.drawLine(nX * cellWidth + cellWidth / 2, nY * cellHeight + cellsLineWidth / 2,
                nX * cellWidth + cellWidth / 2, nY * cellHeight + cellsLineWidth / 2 + cellHeight - cellsLineWidth, paint);
    }

    public void drawAbsolutePath() {
        if ((mainActivity.absolutePath != null)&&(startX>=0)&&(startY>=0)) {
            char[] buffer = new char[mainActivity.absolutePath.length()];
            mainActivity.absolutePath.getChars(0, mainActivity.absolutePath.length(), buffer, 0);

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(COLOR_BLACK);
            paint.setStrokeWidth(cellsLineWidth);
            paint.setStyle(Paint.Style.FILL);

            int sX = startX;//mainActivity.currentX;
            int sY = startY;//mainActivity.currentY;
        //    mCanvas.drawLine(lX+cellWidth/2,lY+cellHeight/2,lX+cellWidth/2,lY+cellHeight+cellHeight/2,paint);

            int cX = sX;
            int cY = sY;
            for (int i = 0; i < mainActivity.absolutePath.length(); i++) {
                String str = new String(String.valueOf(buffer[i]));
                switch (Integer.valueOf(str)) {
                    case 0:
                        Log.i(mainActivity.TAG,"case 0");
                        cX = sX + 1;
                        break;
                    case 1:
                        Log.i(mainActivity.TAG,"case 1");
                        cY = sY + 1;
                        break;
                    case 2:
                        Log.i(mainActivity.TAG,"case 2");
                        cX = sX - 1;
                        break;
                    case 3:
                        Log.i(mainActivity.TAG,"case 3");
                        cY = sY - 1;
                        break;
                }
                mCanvas.drawLine(sX*cellWidth+cellWidth/2,sY*cellHeight+cellHeight/2,
                        cX*cellWidth+cellWidth/2,cY*cellHeight+cellHeight/2,paint);
                sX = cX;
                sY = cY;
            }
        }
    }

    public void drawRobotRiding() {
        if ((mainActivity.previousX != mainActivity.currentX)||(mainActivity.previousY != mainActivity.currentY)) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(COLOR_LIGHT_BLUE);
            paint.setStrokeWidth(cellsLineWidth);
            paint.setStyle(Paint.Style.FILL);

            mCanvas.drawLine(mainActivity.previousX*cellWidth+cellWidth/2,mainActivity.previousY*cellHeight,
                    mainActivity.currentX*cellWidth+cellWidth/2,mainActivity.currentY*cellHeight+cellHeight/2,paint);
        }
    }

    //переводим dp в пиксели
    public float convertDpToPixel(float dp,Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi/160f);
    }

    private void openCallRobotDialog(int X, int Y) {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(mainActivity);
        quitDialog.setTitle("Call robot on these coordinates?");

        final int fX = X;
        final int fY = Y;

        quitDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mainActivity.sendMessage("task",fX,fY,"ride to me");
            }
        });

        quitDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        quitDialog.show();
    }
}

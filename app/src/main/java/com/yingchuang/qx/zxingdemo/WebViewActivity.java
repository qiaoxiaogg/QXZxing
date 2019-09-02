package com.yingchuang.qx.zxingdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.yzq.zxinglibrary.common.Constant;
import com.yzq.zxinglibrary.decode.BitmapLuminanceSource;

import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 * pacakage:com.yingchuang.qx.zxingdemo
 * author:qx
 * date:2019/9/2
 * time:14:09
 * description:
 */
public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private String url="http://appit.winpow.com/app/weixin/erweima.jsp";
    private String imagePath="";

    public static final Set<BarcodeFormat> PRODUCT_FORMATS;
    static final Set<BarcodeFormat> INDUSTRIAL_FORMATS;
    static final Set<BarcodeFormat> ONE_D_FORMATS;
    static final Set<BarcodeFormat> QR_CODE_FORMATS = EnumSet.of(BarcodeFormat.QR_CODE);
    static final Set<BarcodeFormat> DATA_MATRIX_FORMATS = EnumSet.of(BarcodeFormat.DATA_MATRIX);
    static final Set<BarcodeFormat> AZTEC_FORMATS = EnumSet.of(BarcodeFormat.AZTEC);
    static final Set<BarcodeFormat> PDF417_FORMATS = EnumSet.of(BarcodeFormat.PDF_417);

    static {
        PRODUCT_FORMATS = EnumSet.of(BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED);
        INDUSTRIAL_FORMATS = EnumSet.of(BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR);
        ONE_D_FORMATS = EnumSet.copyOf(PRODUCT_FORMATS);
        ONE_D_FORMATS.addAll(INDUSTRIAL_FORMATS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_web);
        webView=findViewById(R.id.webView);

        initWebView();

        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final WebView.HitTestResult htr = ((WebView) v).getHitTestResult();//获取所点击的内容
                if (htr.getType() == WebView.HitTestResult.IMAGE_TYPE //判断被点击的类型为图片
                        || htr.getType() == WebView.HitTestResult.IMAGE_ANCHOR_TYPE
                        || htr.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    imagePath = htr.getExtra();
                    analyzeBitmap(imagePath, new AnalyzeCallback() {
                        @Override
                        public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
                            Intent intent = getIntent();
                            intent.putExtra(Constant.CODED_CONTENT, result);
                            setResult(RESULT_OK, intent);
                            WebViewActivity.this.finish();
                        }

                        @Override
                        public void onAnalyzeFailed() {
                            Toast.makeText(WebViewActivity.this, "图片解析失败，换个图片试试.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return false;
            }
        });
    }

    private Bitmap mBitmap;

    /**
     * 解析二维码图片工具类
     */
    public void analyzeBitmap(final String path, final AnalyzeCallback analyzeCallback) {
        Glide.with(this).load(path).into(new SimpleTarget<Drawable>() {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                BitmapDrawable bd = (BitmapDrawable) resource;
                mBitmap = bd.getBitmap();
                MultiFormatReader multiFormatReader = new MultiFormatReader();
                // 解码的参数
                Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(2);
                // 可以解析的编码类型
                Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>();
                if (decodeFormats == null || decodeFormats.isEmpty()) {
                    decodeFormats = new Vector<BarcodeFormat>();

                    // 这里设置可扫描的类型，我这里选择了都支持
                    decodeFormats.addAll(ONE_D_FORMATS);
                    decodeFormats.addAll(QR_CODE_FORMATS);
                    decodeFormats.addAll(DATA_MATRIX_FORMATS);
                }
                hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
                // 设置继续的字符编码格式为UTF8
                // hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
                // 设置解析配置参数
                multiFormatReader.setHints(hints);
                // 开始对图像资源解码
                Result rawResult = null;
                try {
                    rawResult = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(new BitmapLuminanceSource(mBitmap))));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (rawResult != null) {
                    if (analyzeCallback != null) {
                        analyzeCallback.onAnalyzeSuccess(mBitmap, rawResult.getText());
                    }
                } else {
                    if (analyzeCallback != null) {
                        analyzeCallback.onAnalyzeFailed();
                    }
                }
            }
        });
    }

    /**
     * 解析二维码结果
     */
    public interface AnalyzeCallback {

        void onAnalyzeSuccess(Bitmap mBitmap, String result);

        void onAnalyzeFailed();
    }
    /**
     * @param rawResult 返回的扫描结果
     */
    public void handleDecode(Result rawResult) {
        Intent intent = getIntent();
        intent.putExtra(Constant.CODED_CONTENT, rawResult.getText());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initWebView() {
        WebSettings settings = webView.getSettings();
        //缩放操作
        settings.setSupportZoom(false); //支持缩放，默认为true。是下面那个的前提。
        settings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        settings.setDisplayZoomControls(true); //隐藏原生的缩放控件
        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        settings.setJavaScriptEnabled(true);

        //设置自适应屏幕，两者合用
        settings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        settings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        //其他细节操作
        settings.setCacheMode(WebSettings.LOAD_DEFAULT); //关闭webview中缓存
        settings.setAllowFileAccess(true); //设置可以访问文件
        settings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        settings.setDomStorageEnabled(true);//开启DOM storage API功能
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setLoadsImagesAutomatically(true); //支持自动加载图片
        settings.setBlockNetworkImage(false);//解决图片不显示
        //alert弹窗
        webView.setWebChromeClient(new WebChromeClient());
//        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl(url);
    }
}

package io.dcloud.HelloH5.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.tencent.wework.api.IWWAPI;
import com.tencent.wework.api.IWWAPIEventHandler;
import com.tencent.wework.api.WWAPIFactory;
import com.tencent.wework.api.model.BaseMessage;
import com.tencent.wework.api.model.WWAuthMessage;
import com.tencent.wework.api.model.WWMediaMergedConvs;

import io.dcloud.PandoraEntry;
import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.util.JSUtil;

import org.json.JSONArray;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;

public class QYWXLoginActivity extends Activity implements IFeature{

	IWWAPI iwwapi;
    private static final String APPID = "ww473217aac953cfae";
    private static final String AGENTID = "1000006";
    private static final String SCHEMA = "wwauth473217aac953cfae000006";

    public QYWXLoginActivity(){

	}

	@Override
	public void init(AbsMgr arg0, String arg1) {
		// TODO Auto-generated method stub

	}
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.CUPCAKE)
	@SuppressLint("NewApi")
	@Override
	public String execute(final IWebview pWebview, final String action, final String[] pArgs) {
		// TODO Auto-generated method stub
		//Context context = pWebview.getContext();

		if ("qywxFunction".equals(action))
		{
			final String CallBackID = pArgs[0];
			final JSONArray newArray = new JSONArray();
			newArray.put(pArgs[1]);
			newArray.put(pArgs[2]);
			newArray.put(pArgs[3]);
			newArray.put(pArgs[4]);

			final WWAuthMessage.Req req = new WWAuthMessage.Req();
			req.sch = SCHEMA;
			req.appId = APPID;
			req.agentId = AGENTID;
			req.state = "combest";
			System.out.println("--------------");
			iwwapi.sendMessage(req, new IWWAPIEventHandler() {
				@Override
				public void handleResp(BaseMessage resp) {
					System.out.println("dddddddddd");
					if (resp instanceof WWAuthMessage.Resp) {
						WWAuthMessage.Resp rsp = (WWAuthMessage.Resp) resp;
						if (rsp.errCode == WWAuthMessage.ERR_CANCEL) {
							Toast.makeText(io.dcloud.HelloH5.wxapi.QYWXLoginActivity.this, "登陆取消", Toast.LENGTH_SHORT).show();
						}else if (rsp.errCode == WWAuthMessage.ERR_FAIL) {
							Toast.makeText(io.dcloud.HelloH5.wxapi.QYWXLoginActivity.this, "登陆失败", Toast.LENGTH_SHORT).show();
						} else if (rsp.errCode == WWAuthMessage.ERR_OK) {
							Toast.makeText(io.dcloud.HelloH5.wxapi.QYWXLoginActivity.this, "登陆成功：" + rsp.code,
									Toast.LENGTH_SHORT).show();
							System.out.println("--------------");
							System.out.println(rsp.code);
							JSUtil.execCallback(pWebview, CallBackID, rsp.code, JSUtil.OK, false);
						}
					}
				}
			});

		}
		else if("qywxFunctionSync".equals(action))
		{
			String inValue1 = pArgs[0];
			String inValue2 = pArgs[1];
			String inValue3 = pArgs[2];
			String inValue4 = pArgs[3];

			String ReturnValue = inValue1 + "-" + inValue2 + "-" + inValue3 + "-" + inValue4;
			return JSUtil.wrapJsVar(ReturnValue,true);

		}
		return null;
	}

	@Override
	public void dispose(String arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("enter");
		iwwapi = WWAPIFactory.createWWAPI(this);
		iwwapi.registerApp(SCHEMA);

	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) {
			return;
		}

	}

	@SuppressLint("NewApi")
	public static void closeStrictMode() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.i("close", "服务关闭");
		iwwapi.detach();
		super.onDestroy();
	}

}

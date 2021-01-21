package io.dcloud.HelloH5.wxapi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher.MessageListener;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tencent.wework.api.IWWAPI;
import com.tencent.wework.api.IWWAPIEventHandler;
import com.tencent.wework.api.WWAPIFactory;
import com.tencent.wework.api.model.BaseMessage;
import com.tencent.wework.api.model.WWAuthMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.ProcessMediator;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.DHInterface.IActivityHandler;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.NetTool;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.common.util.ThreadPool;
import io.dcloud.feature.oauth.BaseOAuthService;
import io.dcloud.feature.oauth.weixin.WeiXinMediator;
import io.dcloud.feature.oauth.weixin.WeiXinOAuthService;

public class QyWeiXinOAuthService extends BaseOAuthService {
    private IWWAPI iwwapi;
    private String code;
    private static final String TAG = "QyWeiXinOAuthService";
    private static final String SCHEMA = "wwauth473217aac953cfae000006";
    protected static String appId = null;
    protected static String agentId = null;

    private boolean isAuth = false;
    public QyWeiXinOAuthService(){
    }
    public boolean hasFullConfigData() {
        return !TextUtils.isEmpty(appId);
    }

    public void initAuthOptions(JSONObject mLoginOptions) {
        if (mLoginOptions != null) {
            appId = mLoginOptions.optString("appid", appId);
            Logger.e("QyWeiXinOAuthService", "initAuthOptions: appId" + appId);
            agentId = mLoginOptions.optString("appsecret", agentId);
        }

    }

    public void initMetaData() {
        appId = AndroidResources.getMetaValue("QYWX_APPID");
        agentId = AndroidResources.getMetaValue("QYWX_AGENTID");
    }

    public void init(Context context) {
        super.init(context);
        this.id = "qyweixin";
        this.description = "企业微信";
    }


    private void onLoginCallBack(IWebview pWebViewImpl, String pCallbackId, int code) {
        boolean suc = false;
        String errorMsg = "send";
        if (code == WWAuthMessage.ERR_OK) {
            suc = true;
        } else if (code == WWAuthMessage.ERR_FAIL) {
            errorMsg = "登录失败";
        } else if (code == WWAuthMessage.ERR_CANCEL) {
            this.onLoginFinished(this.getErrorJsonbject(-2, "用户取消"), false, pWebViewImpl, pCallbackId);
            return;
        }

        if (suc) {
            JSUtil.execCallback(pWebViewImpl, pCallbackId, this.makeResultJSONObject(), JSUtil.OK, false);
        } else {
            String msg = DOMException.toJSON(-100, errorMsg, code);
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        }

    }

    public void login(IWebview pWebViewImpl, JSONArray pJsArgs) {
        super.login(pWebViewImpl, pJsArgs);
        if (!this.hasGeneralError(this.mLoginWebViewImpl, this.mLoginCallbackId)) {
            if (!PlatformUtil.isAppInstalled(pWebViewImpl.getContext(), "com.tencent.wework")) {
                String msg = StringUtil.format("{code:%d,message:'%s'}", new Object[]{-8, DOMException.toString("客户端未安装")});
                JSUtil.execCallback(pWebViewImpl, this.mLoginCallbackId, msg, JSUtil.ERROR, true, false);
            } else {
                ThreadPool.self().addThreadTask(new Runnable() {
                    public void run() {
                        QyWeiXinOAuthService.this.loginInThread(QyWeiXinOAuthService.this.mLoginWebViewImpl, QyWeiXinOAuthService.this.mLoginCallbackId, QyWeiXinOAuthService.this.mLoginOptions);
                    }
                });
            }
        }
    }

    public void authorize(IWebview pwebview, JSONArray pJsArgs) {
        super.authorize(pwebview, pJsArgs);
        String msg;
        if (TextUtils.isEmpty(appId)) {
            msg = StringUtil.format("{code:%d,message:'%s'}", new Object[]{-7, DOMException.toString("业务参数配置缺失")});
            JSUtil.execCallback(pwebview, this.mAuthCallbackId, msg, JSUtil.ERROR, true, false);
        } else if (!PlatformUtil.isAppInstalled(pwebview.getContext(), "com.tencent.wework")) {
            msg = StringUtil.format("{code:%d,message:'%s'}", new Object[]{-8, DOMException.toString("客户端未安装")});
            JSUtil.execCallback(pwebview, this.mAuthCallbackId, msg, JSUtil.ERROR, true, false);
        } else {
            ThreadPool.self().addThreadTask(new Runnable() {
                public void run() {
                    QyWeiXinOAuthService.this.isAuth = true;
                    QyWeiXinOAuthService.this.loginInThread(QyWeiXinOAuthService.this.mAuthWebview, QyWeiXinOAuthService.this.mAuthCallbackId, QyWeiXinOAuthService.this.mAuthOptions);
                }
            });
        }
    }

    private void loginInThread(final IWebview pwebview, final String callbackId, JSONObject option) {
        if (this.iwwapi == null) {
            this.iwwapi = WWAPIFactory.createWWAPI(pwebview.getActivity());
            this.iwwapi.registerApp(SCHEMA);
        }
        final WWAuthMessage.Req req = new WWAuthMessage.Req();
        req.sch = SCHEMA;
        req.appId = appId;
        req.agentId = agentId;
        req.state = "combest";

        iwwapi.sendMessage(req, new IWWAPIEventHandler() {
            @Override
            public void handleResp(BaseMessage resp) {
                if (resp instanceof WWAuthMessage.Resp) {
                    WWAuthMessage.Resp rsp = (WWAuthMessage.Resp) resp;
                    if (rsp.errCode == WWAuthMessage.ERR_CANCEL) {
                        Toast.makeText(pwebview.getContext(), "登陆取消", Toast.LENGTH_SHORT).show();
                    }else if (rsp.errCode == WWAuthMessage.ERR_FAIL) {
                        Toast.makeText(pwebview.getContext(), "登陆失败", Toast.LENGTH_SHORT).show();
                    } else if (rsp.errCode == WWAuthMessage.ERR_OK) {
                        Toast.makeText(pwebview.getContext(), "登陆成功：" + rsp.code,
                                Toast.LENGTH_SHORT).show();
                        QyWeiXinOAuthService.this.code = rsp.code;
                        Log.i("qyweixin",rsp.code);
                        JSUtil.execCallback(pwebview, callbackId, QyWeiXinOAuthService.this.makeResultJSONObject(rsp.code), JSUtil.OK, false);
                    }
                }
            }
        });
    }

    protected void onLoginFinished(JSONObject msg, boolean suc, IWebview pwebview, String callbackId) {
        JSUtil.execCallback(pwebview, callbackId, msg, suc ? JSUtil.OK : JSUtil.ERROR, false);
        if (this.isAuth) {
            this.mAuthWebview = null;
            this.mAuthCallbackId = null;
        } else {
            this.mLoginCallbackId = null;
            this.mLoginWebViewImpl = null;
        }

    }
    public void logout(IWebview pWebViewImpl, JSONArray pJsArgs) {
        super.logout(pWebViewImpl, pJsArgs);
        if (!this.hasGeneralError(this.mLogoutWebViewImpl, this.mLogoutCallbackId)) {
            this.removeToken();
            this.userInfo = null;
            this.authResult = null;
            this.onLogoutFinished(this.makeResultJSONObject(), true);
            this.iwwapi = null;
        }
    }

    public JSONObject makeResultJSONObject() {
        JSONObject sucJSON = new JSONObject();

        try {
            sucJSON.put("authResult", this.authResult);
            String state = this.getValue("state");
            sucJSON.put("state", state);
            sucJSON.put("code", this.code);
        } catch (JSONException var3) {
            var3.printStackTrace();
        }

        return sucJSON;
    }

    public JSONObject makeResultJSONObject(String code) {
        JSONObject sucJSON = new JSONObject();

        try {
            String state = this.getValue("state");
            sucJSON.put("state", state);
            sucJSON.put("authResult", code);
        } catch (JSONException var3) {
            var3.printStackTrace();
        }

        return sucJSON;
    }
}

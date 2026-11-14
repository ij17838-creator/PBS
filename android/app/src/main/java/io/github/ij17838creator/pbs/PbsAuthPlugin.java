package io.github.ij17838creator.pbs;

import android.os.CancellationSignal;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/**
 * 앱 안 구글 로그인. 구글은 앱 속 웹 화면(WebView)에서의 로그인 창을 막기 때문에,
 * 안드로이드 기본 계정 선택 창으로 구글 계정을 고르고 받은 ID 토큰을 웹앱에 넘긴다.
 * 웹앱은 그 토큰으로 Firebase에 로그인한다(signInWithCredential).
 * 필요: Firebase 콘솔에 안드로이드 앱(패키지 이름 + 서명 키 SHA-1) 등록, 웹 클라이언트 ID(index.html GOOGLE_WEB_CLIENT_ID).
 */
@CapacitorPlugin(name = "PbsAuth")
public class PbsAuthPlugin extends Plugin {

    @PluginMethod
    public void googleSignIn(PluginCall call) {
        String clientId = call.getString("clientId", "");
        if (clientId == null || clientId.isEmpty()) { call.reject("웹 클라이언트 ID가 없습니다", "no-client-id"); return; }
        if (getActivity() == null) { call.reject("화면이 준비되지 않았습니다"); return; }
        GetSignInWithGoogleOption opt = new GetSignInWithGoogleOption.Builder(clientId).build();
        GetCredentialRequest req = new GetCredentialRequest.Builder().addCredentialOption(opt).build();
        CredentialManager cm = CredentialManager.create(getContext());
        cm.getCredentialAsync(
            getActivity(),
            req,
            new CancellationSignal(),
            ContextCompat.getMainExecutor(getContext()),
            new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                @Override
                public void onResult(GetCredentialResponse res) {
                    Credential c = res.getCredential();
                    if (c instanceof CustomCredential && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(c.getType())) {
                        try {
                            GoogleIdTokenCredential g = GoogleIdTokenCredential.createFrom(((CustomCredential) c).getData());
                            JSObject o = new JSObject();
                            o.put("idToken", g.getIdToken());
                            o.put("email", g.getId());
                            o.put("name", g.getDisplayName());
                            call.resolve(o);
                        } catch (Exception e) {
                            call.reject("구글 로그인 정보를 읽지 못했습니다", "bad-credential");
                        }
                    } else {
                        call.reject("구글 계정이 아닌 로그인 정보입니다", "bad-credential");
                    }
                }

                @Override
                public void onError(GetCredentialException e) {
                    if (e instanceof GetCredentialCancellationException) call.reject("로그인을 취소했습니다", "cancelled");
                    else if (e instanceof NoCredentialException) call.reject("이 기기에 구글 계정이 없거나 앱이 Firebase에 등록되지 않았습니다", "no-credential");
                    else call.reject("구글 로그인 실패: " + e.getMessage(), "failed");
                }
            }
        );
    }

    @PluginMethod
    public void googleSignOut(PluginCall call) {
        CredentialManager.create(getContext()).clearCredentialStateAsync(
            new ClearCredentialStateRequest(),
            new CancellationSignal(),
            ContextCompat.getMainExecutor(getContext()),
            new CredentialManagerCallback<Void, ClearCredentialException>() {
                @Override public void onResult(Void v) { call.resolve(); }
                @Override public void onError(ClearCredentialException e) { call.resolve(); }
            }
        );
    }
}

package com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.injectors;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.WebView;
import com.liskovsoft.browser.Browser;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.events.DecipherOnlySignaturesDoneEvent;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.events.DecipherOnlySignaturesEvent;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.events.GetDecipherCodeDoneEvent;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.events.GetDecipherCodeEvent;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.events.PostDecipheredSignaturesEvent;
import com.liskovsoft.smartyoutubetv.injectors.ResourceInjectorBase;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DecipherSimpleRoutineInjector extends ResourceInjectorBase {
    private String mCombinedSignatures;
    private ArrayList<String> mSignatures;
    private String mDecipherRoutine;
    private String mDecipherCode;
    private List<String> mRawSignatures;

    public DecipherSimpleRoutineInjector(Context context, WebView webView) {
        super(context, webView);
        Browser.getBus().register(this);
    }

    @Subscribe
    public void decipherSignature(DecipherOnlySignaturesEvent event) {
        mRawSignatures = event.getSignatures();
        if (signaturesAreNotCiphered()) {
            Browser.getBus().post(new DecipherOnlySignaturesDoneEvent(mRawSignatures));
            return;
        }

        Browser.getBus().post(new GetDecipherCodeEvent());
    }

    @Subscribe
    public void getDecipherCodeDone(GetDecipherCodeDoneEvent event) {
        mDecipherCode = event.getCode();

        extractSignatures();
        combineSignatures();
        combineDecipherRoutine();
        injectJSContentUnicode(mDecipherRoutine);
    }

    @Subscribe
    public void receiveDecipheredSignatures(PostDecipheredSignaturesEvent event) {
        String[] signatures = event.getSignatures();
        Browser.getBus().post(new DecipherOnlySignaturesDoneEvent(Arrays.asList(signatures)));
    }

    private void combineDecipherRoutine() {
        mDecipherRoutine = mDecipherCode + ";";
        mDecipherRoutine += mCombinedSignatures;
        mDecipherRoutine += "for (var i = 0; i < rawSignatures.length; i++) {rawSignatures[i] = decipherSignature(rawSignatures[i]);}; app.postDecipheredSignatures(rawSignatures);";
    }

    private void extractSignatures() {
        mSignatures = new ArrayList<>();
        for (String rawSignature : mRawSignatures) {
            mSignatures.add(String.format("\"%s\"", rawSignature));
        }
    }

    private void combineSignatures() {
        mCombinedSignatures = "var rawSignatures = [" + TextUtils.join(",", mSignatures) + "];";
    }

    private boolean signaturesAreNotCiphered() {
        return mRawSignatures.size() == 0 || mRawSignatures.get(0) == null;
    }
}

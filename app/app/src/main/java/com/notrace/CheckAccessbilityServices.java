package com.notrace;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.notrace.utils.WebsocketConn;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by notrace on 2016/8/5.
 */
public class CheckAccessbilityServices extends AccessibilityService {
    private boolean webPage = false;
    private Map<String, Long> timeStamp = new HashMap<>();
    private final String LOGTAG = "=====";
    private final Vector<Integer> indexVec = new Vector<>();
    private final Vector<Integer> indexVecBak = new Vector<>();
    private int windowId = -1;
    private boolean needScroll = false;

    private int pageStatus = -1;// 3:main,0:searchMain,1:searchFts,2:contactInfo,-1:invalid
    private String contactId = "lengtoo";
    private String timeStart = "2018年4月2日";
    private String timeEnd = "2018年3月31日";
    private String dateFormate = "yyyy年MM月dd日";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        switch (type) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                if ("com.tencent.mm".equals(event.getPackageName()) && "com.tencent.mm.plugin.webview.ui.tools.WebViewUI".equals(event.getClassName())) {
                    webPage = true;
                    if (windowId == nodeInfo.getWindowId()) {
                        processNextArtical();
                    }
                } else {
                    webPage = false;
                }
                if (isMainPage(event.getClassName().toString())) {
                    pageStatus = 3;
                } else if (isWeixinSearchMainUi(event.getClassName().toString())) {
                    pageStatus = 0;
                } else if (isSearchFtsUi(event.getClassName().toString())) {
                    pageStatus = 1;
                } else if (isWeixinContactInfoPage(event.getClassName().toString())) {
                    pageStatus = 2;
                } else {
                    pageStatus = -1;
                }
                if ("com.tencent.mm.ui.chatting.ChattingUI".equals(event.getClassName().toString())) {
                    if (!timeStamp.containsKey("点击返回") || System.currentTimeMillis() - timeStamp.get("点击返回") > 800) {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                    }
                    timeStamp.put("点击返回", System.currentTimeMillis());
                }
                if (nodeInfo != null) {
                    final List<AccessibilityNodeInfo> copy = nodeInfo.findAccessibilityNodeInfosByText("复制链接");
                    if (copy != null && copy.size() > 0) {
                        clickOnMenuCopyUrlLink();
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:

                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                try {
                    if (pageStatus == 0) {
                        //点击搜索公众号
                        clickSearchFts(getRootInActiveWindow());
                        return;
                    }
                    if (pageStatus == 1) {
                        //输入内容
                        pasteWeixinContactId(getRootInActiveWindow());
                        return;
                    }
                    if (pageStatus == 2) {
                        //采集公众号资料
                        collectContactInfo(getRootInActiveWindow());
                        //关注
                        boolean result = addFocus(getRootInActiveWindow());
                        //查看历史消息
                        if (!result) {
                            clickOnHistoryMessage(getRootInActiveWindow());
                        }
                        return;
                    }
                    if (pageStatus == 3) {
                        //点击搜索
                        clickSearch(getRootInActiveWindow());
                        return;
                    }
                    if (webPage && "android.webkit.WebView".equals(event.getClassName())) {
                        if (isWeixinArticalList(event.getSource())) {
                            clickNextNode(event.getSource());
                        } else if (isWeixinArticalDetail(event.getSource())) {
                            Log.e(LOGTAG, "isWeixinArticalDetail");
                            clickOnMenuMore();
                        } else if (isIllegalePage(event.getSource())) {
                            performGlobalAction(GLOBAL_ACTION_BACK);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
        }
    }

    /**
     * 点击查看历史消息
     *
     * @param rootInActiveWindow
     */
    private void clickOnHistoryMessage(AccessibilityNodeInfo rootInActiveWindow) {
        List<AccessibilityNodeInfo> viewHistory = rootInActiveWindow.findAccessibilityNodeInfosByText("查看历史消息");
        if (viewHistory != null && viewHistory.size() > 0) {
            if (!timeStamp.containsKey("查看历史消息") || System.currentTimeMillis() - timeStamp.get("查看历史消息") > 800) {
                viewHistory.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            timeStamp.put("查看历史消息", System.currentTimeMillis());
        }
    }

    /**
     * 关注公众号
     *
     * @param rootInActiveWindow
     * @return
     */
    private boolean addFocus(AccessibilityNodeInfo rootInActiveWindow) {
        boolean ret = false;
        List<AccessibilityNodeInfo> addFocus = rootInActiveWindow.findAccessibilityNodeInfosByText("关注");
        if (addFocus != null && addFocus.size() > 0) {
            for (AccessibilityNodeInfo node : addFocus) {
                if ("关注".equals(node.getText().toString())) {
                    if (!timeStamp.containsKey("关注公众号") || System.currentTimeMillis() - timeStamp.get("关注公众号") > 800) {
                        node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        ret = true;
                    }
                    timeStamp.put("关注公众号", System.currentTimeMillis());
                }
            }
        }
        return ret;
    }

    /**
     * 采集公众号信息
     *
     * @param rootInActiveWindow
     */
    private void collectContactInfo(AccessibilityNodeInfo rootInActiveWindow) {
        List<AccessibilityNodeInfo> accessNodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId("android:id/summary");
        if (accessNodes != null && accessNodes.size() > 0) {
            accessNodes.get(0).getText();
            // TODO: 2018/4/5 发送
            contactId = "";
        }
    }

    /**
     * 粘贴公众号id
     *
     * @param rootInActiveWindow
     */
    private void pasteWeixinContactId(AccessibilityNodeInfo rootInActiveWindow) {
        List<AccessibilityNodeInfo> accessNodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ht");
        if (accessNodes != null && accessNodes.size() > 0) {
            if (contactId.equals(accessNodes.get(0).getText().toString())) {
                AccessibilityNodeInfo webviewNode = findWebViewNode(rootInActiveWindow);
                if (webviewNode != null) {
                    AccessibilityNodeInfo node = findContactNode(webviewNode, contactId);
                    if (node != null) {
                        if (!timeStamp.containsKey("点击公众号") || System.currentTimeMillis() - timeStamp.get("点击公众号") > 800) {
                            JSONObject jsonObject = new JSONObject();
                            Rect rect = new Rect();
                            node.getParent().getBoundsInScreen(rect);
                            try {
                                jsonObject.put("type", "cmd");
                                jsonObject.put("content", String.format("adb shell input tap %d %d", (rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2));
                                WebsocketConn.sendMessage(jsonObject.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        timeStamp.put("点击公众号", System.currentTimeMillis());
                    }
                }
            } else {
                if (!timeStamp.containsKey("粘贴公众号") || System.currentTimeMillis() - timeStamp.get("粘贴公众号") > 800) {
                    if (TextUtils.isEmpty(contactId)) {
                        return;
                    }
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("content", contactId);
                    clipboard.setPrimaryClip(clip);
                    accessNodes.get(0).performAction(AccessibilityNodeInfo.FOCUS_INPUT);
                    accessNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_PASTE);
//                    // adb shell input keyevent 66
//                    JSONObject jsonObject = new JSONObject();
//                    try {
//                        jsonObject.put("type", "cmd");
//                        jsonObject.put("content", "adb shell input keyevent 66");//String.format("adb shell input tap %d %d", (rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2));
//                        WebsocketConn.sendMessage(jsonObject.toString());
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }
                timeStamp.put("粘贴公众号", System.currentTimeMillis());
            }
        }
    }


    private AccessibilityNodeInfo findContactNode(AccessibilityNodeInfo node, String contactId) {
        if (node == null || String.format("微信号：%s", contactId).equals(node.getContentDescription()) || String.format("微信号：%s已关注", contactId).equals(node.getContentDescription())) {
            return node;
        }

        AccessibilityNodeInfo ret = null;
        for (int i = 0; i < node.getChildCount(); i++) {
            ret = findContactNode(node.getChild(i), contactId);
            if (ret != null) {
                break;
            }
        }
        return ret;
    }


    /**
     * 点击搜索公众号
     *
     * @param rootInActiveWindow
     */
    private void clickSearchFts(AccessibilityNodeInfo rootInActiveWindow) {
        List<AccessibilityNodeInfo> accessNodes = rootInActiveWindow.findAccessibilityNodeInfosByText("公众号");
        if (accessNodes != null && accessNodes.size() > 0) {
            if (!timeStamp.containsKey("搜索公众号") || System.currentTimeMillis() - timeStamp.get("搜索公众号") > 800) {
                accessNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            timeStamp.put("搜索公众号", System.currentTimeMillis());
        }
    }

    /**
     * 点击搜索
     *
     * @param rootInActiveWindow
     */
    private void clickSearch(AccessibilityNodeInfo rootInActiveWindow) {
        List<AccessibilityNodeInfo> accessNodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/gx");
        AccessibilityNodeInfo searchNode = null;
        if (accessNodes != null && accessNodes.size() > 0) {
            for (int i = 0; i < accessNodes.size(); i++) {
                searchNode = findSearchBtn(accessNodes.get(i));
                if (searchNode != null) {
                    break;
                }
            }
        }
        if (searchNode != null) {
            if (!timeStamp.containsKey("搜索") || System.currentTimeMillis() - timeStamp.get("搜索") > 800) {
                searchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            timeStamp.put("搜索", System.currentTimeMillis());
        }
    }

    private AccessibilityNodeInfo findSearchBtn(AccessibilityNodeInfo node) {
        if (node == null || TextUtils.equals("搜索", node.getContentDescription())) {
            return node;
        }
        AccessibilityNodeInfo ret = null;
        for (int i = 0; i < node.getChildCount(); i++) {
            ret = findSearchBtn(node.getChild(i));
            if (ret != null) {
                return ret;
            }
        }

        return ret;
    }

    private void processNextArtical() {
        final AccessibilityNodeInfo node = findWebViewNode(getRootInActiveWindow());
        if(node==null) {
            return;
        }
        if (needScroll) {
            needScroll = false;
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (node != null) {
                        clickNextNode(node);
                    }
                }
            }, 1000);
        } else {
            if (node != null) {
                clickNextNode(node);
            }
        }
    }

    private void clickNextNode(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (!timeStamp.containsKey("getNextAritvalItem") || System.currentTimeMillis() - timeStamp.get("getNextAritvalItem") > 1000) {
            Log.e(LOGTAG, "isWeixinArticalList");
            if (windowId != accessibilityNodeInfo.getWindowId()) {
                windowId = accessibilityNodeInfo.getWindowId();
                indexVec.clear();
            }
            indexVecBak.clear();
            indexVecBak.addAll(indexVec);
            Rect pageRect = new Rect();
            accessibilityNodeInfo.getBoundsInScreen(pageRect);
            final AccessibilityNodeInfo node = getNextAritvalItem(accessibilityNodeInfo);
            if (node != null) {
                List<String> nodeinfo = getNodeinfo(node);
                Log.e(LOGTAG, indexVec.toString() + "\t" + nodeinfo.toString());
                Rect rect = new Rect();
                node.getBoundsInScreen(rect);
                if ((pageRect.bottom - rect.bottom) < Math.abs((rect.bottom - rect.top) / 2)) {
//                if (rect.bottom >= pageRect.bottom) {
                    needScroll = true;
                }
                boolean hasDate = false;
                try {
                    Date dateStart = new SimpleDateFormat(dateFormate).parse(timeStart);
                    Date dateEnd = new SimpleDateFormat(dateFormate).parse(timeEnd);
                    String eL = "^\\d{4}年\\d{1,2}月\\d{1,2}日$";
                    Pattern p = Pattern.compile(eL);
                    for(String s:nodeinfo) {
                        Matcher m = p.matcher(s);
                        boolean b = m.matches();
                        if (b) {
                            hasDate = true;
                            Date date = new SimpleDateFormat(dateFormate).parse(s);
                            if(date.before(dateStart)&&date.after(dateEnd)) {
                                processNextArtical();
                                return;
                            }
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if(hasDate==false && nodeinfo.contains("正在加载")) {
                    indexVec.clear();
                    indexVec.addAll(indexVecBak);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            processNextArtical();
                        }
                    }, 3000);
                    return;
                }
                Log.e(LOGTAG, rect.toString());
                // 调用adb 进行模拟点击
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "cmd");
                    jsonObject.put("content", String.format("adb shell input tap %d %d", (rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2));
                    JSONArray jsonArray = new JSONArray();
                    for (String s : nodeinfo) {
                        jsonArray.put(s);
                    }
                    jsonObject.put("data", jsonArray);
                    WebsocketConn.sendMessage(jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        timeStamp.put("getNextAritvalItem", System.currentTimeMillis());
    }

    private AccessibilityNodeInfo findWebViewNode(AccessibilityNodeInfo accessibilityNodeInfo) {
        AccessibilityNodeInfo ret = null;
        if (accessibilityNodeInfo != null) {
            for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo child = accessibilityNodeInfo.getChild(i);
                if (child == null) {
                    continue;
                }
                if (child != null && "android.webkit.WebView".equals(child.getClassName())) {
                    return child;
                }
                if (child.getChildCount() > 0) {
                    ret = findWebViewNode(child);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
        }
        return ret;
    }

    private List<String> getNodeinfo(AccessibilityNodeInfo nodeInfo) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            if (!TextUtils.isEmpty(nodeInfo.getChild(i).getContentDescription())) {
                strings.add(nodeInfo.getChild(i).getContentDescription().toString());
            }
        }
        return strings;
    }

    private AccessibilityNodeInfo getNextAritvalItem(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo from = root;
        for (int i = 0; i < indexVec.size() - 1; i++) {
            if (from.getChildCount() > indexVec.get(i)) {
                from = from.getChild(indexVec.get(i) >= 0 ? indexVec.get(i) : 0);
            } else {
                return null;
            }
        }
        AccessibilityNodeInfo ret = null;
        if (indexVec.size() == 0) {
            indexVec.add(-1);
        }
        do {
            if (from != null && from.getChildCount() > indexVec.get(indexVec.size() - 1)) {
                int offset = indexVec.remove(indexVec.size() - 1) + 1;
                ret = findfirstArticalItem(from, offset);
            }
            if (ret == null) {
                from = from.getParent();
                if (indexVec.size() > 0) {
                    if (from.getChildCount() > indexVec.get(indexVec.size() - 1) + 1) {
                        indexVec.set(indexVec.size() - 1, indexVec.get(indexVec.size() - 1) + 1);
                        from = from.getChild(indexVec.get(indexVec.size() - 1));
                        indexVec.add(-1);
                    }
                }
            }
        } while (ret == null && indexVec.size() > 0);
        return ret;
    }

    private AccessibilityNodeInfo findfirstArticalItem(AccessibilityNodeInfo accessibilityNodeInfo, int offset) {
        int result = isArticalItemNode(accessibilityNodeInfo);
        if (result == 0) {
            return accessibilityNodeInfo;
        } else if (result == 1) {
            for (int i = offset; i < accessibilityNodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo node = accessibilityNodeInfo.getChild(i);
                if (node.getChildCount() > 0) {
                    indexVec.add(i);
                    AccessibilityNodeInfo accessibilityNodeInfo1 = findfirstArticalItem(node, 0);
                    if (accessibilityNodeInfo1 != null) {
                        return accessibilityNodeInfo1;
                    } else {
                        indexVec.remove(indexVec.size() - 1);
                    }
                }
            }
        }
        return null;
    }

    // 点击更多
    private void clickOnMenuMore() {
        List<AccessibilityNodeInfo> moreInfos = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/he");
        if (moreInfos != null && moreInfos.size() > 0) {
            if (!timeStamp.containsKey("更多") || System.currentTimeMillis() - timeStamp.get("更多") > 800) {
                moreInfos.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            timeStamp.put("更多", System.currentTimeMillis());
        }
    }

    // 返回0：当前节点是微信文章item，1:可以向下一级递归 -1：不需要向下一级递归
    private int isArticalItemNode(AccessibilityNodeInfo accessibilityNodeInfo) {
        // 判断规则：如果有大于一个子节点，并且每个子节点都没有子节点，返回0
        // 如果子节点也有子节点，返回1
        // 其他返回-1
        if (accessibilityNodeInfo != null && accessibilityNodeInfo.getChildCount() > 0) {
            int count = 0;
            for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
                if (accessibilityNodeInfo.getChild(i) !=null && accessibilityNodeInfo.getChild(i).getChildCount() > 0) {
                    count++;
                }
            }
            if (count == 0 && accessibilityNodeInfo.getChildCount() > 1) {
                return 0;
            }

            if (count > 0) {
                return 1;
            }
        }
        return -1;
    }

    // 点击渎职链接
    private void clickOnMenuCopyUrlLink() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        List<AccessibilityNodeInfo> moreInfos = nodeInfo.findAccessibilityNodeInfosByText("复制链接");
        if (moreInfos != null && moreInfos.size() > 0) {
            if (!timeStamp.containsKey("复制链接") || System.currentTimeMillis() - timeStamp.get("复制链接") > 800) {
                moreInfos.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 获取系统剪贴板
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        // 获取剪贴板的剪贴数据集
                        ClipData clipData = clipboard.getPrimaryClip();
                        if (clipData != null && clipData.getItemCount() > 0) {
                            // 从数据集中获取（粘贴）第一条文本数据
                            CharSequence text = clipData.getItemAt(0).getText();
                            if (!TextUtils.isEmpty(text)) {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("type", "insert");
                                    jsonObject.put("data", text);
                                    WebsocketConn.sendMessage(jsonObject.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        performGlobalAction(GLOBAL_ACTION_BACK);
                    }
                }, 800);
            }
            timeStamp.put("复制链接", System.currentTimeMillis());
        }
    }

    private boolean isMainPage(String className) {
        if ("com.tencent.mm.ui.LauncherUI".equals(className)) {
            return true;
        }
        return false;
    }

    private boolean isWeixinSearchMainUi(String className) {
        if ("com.tencent.mm.plugin.search.ui.FTSMainUI".equals(className)) {
            return true;
        }
        return false;
    }

    private boolean isSearchFtsUi(String className) {
        if ("com.tencent.mm.plugin.webview.ui.tools.fts.FTSSearchTabWebViewUI".equals(className)) {
            return true;
        }
        return false;
    }

    private boolean isWeixinContactInfoPage(String className) {
        if ("com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(className)) {
            return true;
        }
        return false;
    }


    private boolean isWeixinArticalList(AccessibilityNodeInfo source) {
        if (source != null && source.getChildCount() > 0 && source.getChild(0) != null && TextUtils.equals(source.getChild(0).getContentDescription(), "搜索公众号文章 搜索")) {
            return true;
        }
        return false;
    }

    private boolean isWeixinArticalDetail(AccessibilityNodeInfo source) {
        if (source != null && source.getChildCount() > 0 && source.getChild(0) != null) {
            AccessibilityNodeInfo node = source.getChild(0).getChild(1);
            if (node != null) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    if ("post-user".equals(node.getChild(i).getViewIdResourceName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 是否违规页面
     *
     * @param source
     * @return
     */
    private boolean isIllegalePage(AccessibilityNodeInfo source) {
        if (source != null && source.getChildCount() > 1) {
            if (source.getChild(1) != null && "此内容因违规无法查看".equals(source.getChild(1).getContentDescription())) {
                return true;
            }
            for (int i = 1; i < source.getChildCount(); i++) {
                if (source.getChild(i) != null && "此内容因违规无法查看".equals(source.getChild(i).getContentDescription())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        Log.d("=====", "clickservice中断");
        Toast.makeText(this, "中断点击", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("=====", "clickserviceonCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("=====", "clickserviceonDestroy");
    }
}

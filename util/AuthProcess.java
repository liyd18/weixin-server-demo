package net.jeeshop.web.action.manage.wht.wxaccount.util;

import javax.servlet.http.HttpServletRequest;

public class AuthProcess {
	public final static String Token = "wanhutong123";//公众平台上面自己填写的Token  
	public final static String EncodingAESKey = "TQ207W9eBLFliMydheEa9YlEPaZ3XI0ptNeLrdhprKW";//公众平台上面自己填写的43位EncodingAESKey  
    public final static String AppID = "wxde98fa391728deca";//应用的appid（微信生成的）  
  
    /** 
    * 将加密后的原文进行解密重新封装 
    * @param request 
    * @param originalXml 原xml 
    * @return    重新解密后的xml 
     */  
    public static String  decryptMsg(HttpServletRequest request,String originalXml) {  
        // 微信加密签名   
        //String sVerifyMsgSig = request.getParameter("signature");  
        String msgSignature = request.getParameter("msg_signature");  
        // 时间戳  
        String timestamp = request.getParameter("timestamp");  
        // 随机数  
        String nonce = request.getParameter("nonce");  
        try {  
            WXBizMsgCrypt pc = new WXBizMsgCrypt(Token, EncodingAESKey, AppID);  
            return pc.decryptMsg(msgSignature, timestamp, nonce, originalXml);  
        } catch (AesException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
        return null;  
    }  
    /** 
     * 对需要回复的原文进行加密重新封装 
     * @param request 
     * @param replyXml 需要回复的xml 
     * @return    重新加密后的xml 
      */  
     public static String  encryptMsg(HttpServletRequest request,String replyXml) {  
         // 时间戳  
         String timestamp = request.getParameter("timestamp");  
         // 随机数  
         String nonce = request.getParameter("nonce");  
         try {  
             WXBizMsgCrypt pc = new WXBizMsgCrypt(Token, EncodingAESKey, AppID);  
             return pc.encryptMsg(replyXml, timestamp, nonce);  
         } catch (AesException e) {  
             // TODO Auto-generated catch block  
             e.printStackTrace();  
         }  
         return null;  
     }  
}

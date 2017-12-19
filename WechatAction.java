package net.jeeshop.web.action.manage.wht.wxaccount;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jeeshop.services.manage.wht.buyAdviserToOpenID.BuyAdviserToOpenIDService;
import net.jeeshop.services.manage.wht.userBase.UserConstService;
import net.jeeshop.services.manage.wht.wxaccount.WxaccountService;
import net.jeeshop.services.manage.wht.wxaccount.bean.Wxaccount;
import net.jeeshop.web.action.manage.wht.wxaccount.util.AuthProcess;
import net.jeeshop.web.util.SignUtil;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.wanhutong.user.api.bean.BuyAdviserToOpenID;

@Controller
@RequestMapping("/whtWechatAction")
public class WechatAction { 
    
	private final Logger log = LoggerFactory.getLogger(WechatAction.class);
	
	@Resource(name="whtWxaccountServiceManage")
	private WxaccountService service;
	@Autowired
	private BuyAdviserToOpenIDService buyAdviserToOpenIDService;
	@Autowired
	private UserConstService userConstService;
	
	/**
	 * 请求校验（确认请求来自微信服务器）
	 * @throws Exception 
	 */
	@RequestMapping("doGet")
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  {
		request.setCharacterEncoding("UTF-8");  //微信服务器POST消息时用的是UTF-8编码，在接收时也要用同样的编码，否则中文会乱码；
        response.setCharacterEncoding("UTF-8"); //在响应消息（回复消息给用户）时，也将编码方式设置为UTF-8，原理同上；
		PrintWriter out = response.getWriter();
		String xml = getString(request);
		//加密消息处理  
		String encrypt_type =request.getParameter("encrypt_type");  
		try {
			String openid = request.getParameter("openid");
			if(StringUtils.isBlank(openid)){
				log.error(">>>>>>>>>>>>>>>>openID为空");
			}
			if (encrypt_type == null || encrypt_type.equals("raw")) {	//不需要加密  
				log.error("调试明文信息: " + xml);
				log.info(">>>>>>>>>>>>>>明文模式");
				echostr(request, response, "0");
				String message=insert(xml, openid);
				if(!StringUtils.isBlank(message)){
					out.write(message);
				}
			} else {	//需走加解密流程  
				log.info(">>>>>>>>>>>>>>加密模式");
				echostr(request, response, "1");
				String msg = AuthProcess.decryptMsg(request, xml);//解密请求消息体
				log.error("调试解密信息:" + msg);
				String message=insert(msg, openid);
				if(!StringUtils.isBlank(message)){
					//加密
					message=AuthProcess.encryptMsg(request, message);
					out.write(message);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	private void  echostr(HttpServletRequest request, HttpServletResponse response,String status){
		String signature = null;
		if("0".equals(status)){
			// 微信加密签名
			signature = request.getParameter("signature");
		}else{
			// 微信加密签名
			signature = request.getParameter("msg_signature");  
		}
		// 时间戳
		String timestamp = request.getParameter("timestamp");
		// 随机数
		String nonce = request.getParameter("nonce");
		// 随机字符串
		String echostr = request.getParameter("echostr");
		PrintWriter out;
		try {
			out = response.getWriter();
			//请求校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败
			if (SignUtil.checkSignature(signature, timestamp, nonce)) {
				out.print(echostr);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String insert(String msg,String openid){
		String respMessage = "";
		log.info("------------------openid:"+openid);
		Map<String, Object> map = null;
	    if(StringUtils.isNotBlank(msg)){
	    	map = getMap(msg);
	    }
	    //发送方帐号（open_id）
        String  fromUserName= (String) map.get("FromUserName");
        //公众帐号
        String  toUserName= (String) map.get("ToUserName");
        //创建时间
        String createTime=(String) map.get("CreateTime");
        //消息类型
        String msgType = (String) map.get("MsgType");
        //消息内容
        String content = (String) map.get("Content");
	    if(map!=null){
	    	 String s = (String) map.get("EventKey");
	    	 log.info("------------------二维码参数:"+s);
	         String even = (String) map.get("Event");
	         if(msgType.equals("text")) {
            	log.info(">>>>>>>进入【文本消息】");
            	if(content.equalsIgnoreCase("报名") || content.equalsIgnoreCase("baoming")){
            		respMessage=autoReply(toUserName, fromUserName,createTime);
            		log.info("--------------"+respMessage);
                }
            	if(content.indexOf("奖")>=0 || content.indexOf("jiang")>=0){
            		respMessage=attention(toUserName, fromUserName,createTime);
            		log.info("--------------"+respMessage);
                }
            	return respMessage;
        	}else if("subscribe".equals(even)){	//扫码关注
        		log.error("调试扫码关注==========");
        		//如果采购顾问使用新的系统 
	        	if (StringUtils.equals("1", userConstService.adviserSys)) {
					return this.linkAdviserNe(s, fromUserName, toUserName, createTime);
				} else {
					//采购顾问使用旧系统
					Wxaccount w = service.selectById(openid);
					if(w==null){	//判断是否第一次关注
						if(StringUtils.isNotBlank(s)){	
							String[] split = s.split("_");
							try {
								Wxaccount e = new Wxaccount();
								e.setOpenid((String) map.get("FromUserName"));
								e.setAccountid(Integer.valueOf(split[1]));
								e.setWxaccount((String) map.get("ToUserName"));
								e.setTicket((String) map.get("Ticket"));
								e.setStatus("0");
								service.insert(e);
							} catch (NumberFormatException e) {
								log.error(">>>>>>>>>>>>>>添加数据失败");
								e.printStackTrace();
							}
							log.info(">>>>>>>>>>>>>>关注成功");
						}
						respMessage=attention(toUserName, fromUserName,createTime);
						log.info("--------------"+respMessage);
						return respMessage;
					}else if("1".equals(w.getStatus())){  //如果之前关注过，二维码参数不同，修改采购顾问和状态
						if(StringUtils.isNotBlank(s)){	
							String[] split = s.split("_");
							try {
								Wxaccount e = new Wxaccount();
								e.setOpenid(openid);
								e.setStatus("0");
								e.setAccountid(Integer.valueOf(split[1]));
								service.updateSucceedType(e);
							} catch (NumberFormatException e) {
								log.error(">>>>>>>>>>>>>更新失败");
								e.printStackTrace();
							}
							log.info(">>>>>>>>>>>>>>重新关注");
						}
						respMessage=attention(toUserName, fromUserName,createTime);
						log.info("--------------"+respMessage);
						return respMessage;
					}
				}
	         }else if("unsubscribe".equals(even)){	//取消关注
	        	 log.error("调试取消关注==========");
	        	//如果采购顾问使用新的系统 
	        	if (StringUtils.equals("1", userConstService.adviserSys)) {
					this.delinkAdviserNe(fromUserName);
				} else {
					//采购顾问使用旧系统
					Wxaccount w = service.selectById(openid);
					if("0".equals(w.getStatus())){
						try {
							Wxaccount e = new Wxaccount();
							e.setOpenid((String) map.get("FromUserName"));
							e.setStatus("1");
							service.updateSucceedType(e);
						} catch (Exception e) {
							log.error(">>>>>>>>>>>>>>更新失败");
							e.printStackTrace();
						}
						log.info(">>>>>>>>>>>>>>取消关注");
					}
				}
	         	return respMessage;
	         }
	    }
	    return respMessage;
	}
	
	private String getString(HttpServletRequest request){
		// TODO 消息的接收、处理、响应
		ServletInputStream stream;
		String xml = null;
		try {
			stream = request.getInputStream();
			StringBuilder content = new StringBuilder();  
		    byte[] b = new byte[1024];  
		    int lens = -1;  
		    while ((lens = stream.read(b)) > 0) {  
		        content.append(new String(b, 0, lens));  
		    }  
		    xml = content.toString();// 内容
		    
		} catch (IOException e) {
			e.printStackTrace();
		}
		return xml;
	}
	
	//将xml格式的字符串转换成map
	private Map<String, Object> getMap(String xmlStr){
		 Map<String, Object> map = new HashMap<String, Object>();  
	        //将xml格式的字符串转换成Document对象  
	        Document doc = null;
			try {
				doc = (Document) DocumentHelper.parseText(xmlStr);
			} catch (DocumentException e) {
				e.printStackTrace();
			}  
	        //获取根节点  
	        Element root = doc.getRootElement();  
	        //获取根节点下的所有元素  
	        List children = root.elements();  
	        //循环所有子元素  
	        if(children != null && children.size() > 0) {  
	            for(int i = 0; i < children.size(); i++) {  
	                Element child = (Element)children.get(i);  
	                map.put(child.getName(), child.getTextTrim());  
	            }  
	        }
			return map;  
	}
	
	/**
	 * 
	 * <p>Description: 自动回复(报名活动)</br>
	 * @author wangy
	 * @param toUserName
	 * @param fromUserName
	 * @return
	 */
	private String autoReply(String toUserName,String fromUserName,String createTime){
		String str="<xml>"+
				  "<ToUserName>"+fromUserName+"</ToUserName>"+
				  "<FromUserName>"+toUserName+"</FromUserName>"+
				  "<CreateTime>"+createTime+"</CreateTime>"+
				  "<MsgType>news</MsgType>"+
				  "<ArticleCount>1</ArticleCount>"+
				  "<Articles>"+
				    "<item>"+
				      "<Title>第三届中国（白沟）国际箱包皮具交易博览会开始报名了</Title>"+
				      "<Description>第三届中国（白沟）国际箱包皮具交易博览会将于2017年9月21日-23日在河北白沟和道国际箱包展览中心隆重举行！博览会为国内外箱包采购商提供新品、及流行趋势发布，与箱包厂家建立直接采购关系，稳定货源，掌握行业发展趋势。</Description>"+
				      "<PicUrl>http://cms.wanhutong.com/1500089200641030785.png</PicUrl>"+
				      "<Url>http://baoming.wanhutong.com/v/U80718WT324J</Url>"+
				    "</item>"+
				  "</Articles>"+
				"</xml>";
		return str;
	}

	/**
	 * 关联采购顾问
	 */
	private String linkAdviserNe(String eventKey, String fromUserName, String toUserName, String createTime) {
		String openID = fromUserName;
		String appNO = toUserName;
		BuyAdviserToOpenID adviserToOpenID = buyAdviserToOpenIDService.selectByOpenID(openID);
		//如果为空，认为未关注或者已经取消关注
		if (adviserToOpenID == null) {
			try {
				if(StringUtils.isNotBlank(eventKey)){	
					String[] split = eventKey.split("_");
					Integer adviserID = Integer.valueOf(split[1]);
					BuyAdviserToOpenID param = new BuyAdviserToOpenID();
					param.setBuyerOpenID(openID);
					param.setAdviserID(adviserID);
					param.setAppNO(appNO);
					Integer id = buyAdviserToOpenIDService.insert(param);
					if (id != null && id > 0) {
						log.info(">>>>>>>>>>>>>>关注成功");
					} else {
						log.error(">>>>>>>>>>>>>>添加数据失败");
					}
				}
			} catch (Exception e) {
				log.error(">>>>>>>>>>>>>>关联采购顾问异常: " + e);
			}
			String respMessage = attention(toUserName, fromUserName, createTime);
			log.info("--------------"+respMessage);
     		return respMessage;
		} else {
			//已关联采购顾问，且未取消关联，不做任何处理
		}
		return null;
	}
	
	/**
	 * 取消关联采购顾问
	 */
	private void delinkAdviserNe(String openID) {
		try {
			BuyAdviserToOpenID adviserToOpenID = buyAdviserToOpenIDService.selectByOpenID(openID);
			if (adviserToOpenID != null && adviserToOpenID.getId() != null) {
				Integer row = buyAdviserToOpenIDService.deleteByID(adviserToOpenID.getId());
				//结果不为空，且大于0，认为删除成功
				if (row != null && row > 0) {
					log.info(">>>>>>>>>>>>>>取消关注");
				} else {
					log.error(">>>>>>>>>>>>>>更新失败");
				}
			}
		} catch (Exception e) {
			log.error(">>>>>>>>>>>>>>取消关注异常: " + e);
		}
	}
	
	/**
	 * <p>Description: TODO(微信抽奖)</br>
	 * @author liyd
	 * @param toUserName
	 * @param fromUserName
	 * @param createTime
	 * @return
	 */
	private String attention(String toUserName,String fromUserName,String createTime){
		String str="<xml>"+
				  "<ToUserName>"+fromUserName+"</ToUserName>"+
				  "<FromUserName>"+toUserName+"</FromUserName>"+
				  "<CreateTime>"+createTime+"</CreateTime>"+
				  "<MsgType>news</MsgType>"+
				  "<ArticleCount>1</ArticleCount>"+
				  "<Articles>"+
				    "<item>"+
				      "<Title>万户通产品发布会现场抽奖(2017年9月21日)</Title>"+
				      "<Description>欢迎光临万户通产品发布会，我们将于9月21日14:35分向大家派送5000元代金券大奖，欢迎各位采购商前来参与。</Description>"+
				      "<PicUrl>http://media.hedaoyuncang.com/20170919175813.png</PicUrl>"+
				      "<Url>http://wxshop.wanhutong.com/shop/lottery/faword</Url>"+
				    "</item>"+
				  "</Articles>"+
				"</xml>";
		return str;
	}
	
	
	
	
}

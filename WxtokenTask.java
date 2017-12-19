package net.jeeshop.web.action.manage.wht.wxtoken;

import javax.annotation.Resource;

import me.chanjar.weixin.mp.api.WxMpService;
import net.jeeshop.services.manage.wht.wxtoken.WxtokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WxtokenTask {
	@Autowired
	private WxMpService wxMpService;
	@Resource(name="whtWxtokenServiceManage")
	private WxtokenService service;
	
//	@Scheduled(fixedRate=7000000)
//	public void getToken(){
//		String appID="wxde98fa391728deca";
//		try {
//			String token = wxMpService.getAccessToken(true);
//			Wxtoken w = new Wxtoken();
//			w.setAppid(appID);
//			w.setAccesstoken(token);
//			w.setExpires(7200);
//			long timeNow= System.currentTimeMillis();
//			long timeLose=System.currentTimeMillis()+2*60*20*1000;
//			w.setCreatetime(new Date(timeNow));
//			w.setExpirestime(new Date(timeLose));
//			service.insert(w);
//		} catch (Exception e) {
//				e.printStackTrace();
//		}
//	}

}

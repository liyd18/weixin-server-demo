package net.jeeshop.web.action.manage.wht.wxtoken;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import net.jeeshop.services.manage.wht.wxtoken.WxtokenService;
import net.jeeshop.services.manage.wht.wxtoken.bean.Wxtoken;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/wht/WxtokenAction")
public class WxtokenAction {

	@Resource(name="whtWxtokenServiceManage")
	private WxtokenService service;
	
	@RequestMapping(value = "getToken",method = RequestMethod.POST)
	@ResponseBody
	public Wxtoken getToken(HttpServletResponse response){
		response.setHeader("Access-Control-Allow-Origin","*");
		response.setHeader("Access-Control-Allow-Methods","POST");
		response.setHeader("Access-Control-Allow-Headers","Access-Control");
		response.setHeader("Allow","POST");
		Wxtoken wxtoken = service.selectOne(null);
		return wxtoken;
	}
}

package net.jeeshop.web.action.manage.wht.wxaccount;


import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("lottery")
public class LotteryAction {

	@RequestMapping("faword")
	public void faword(HttpServletResponse response) throws IOException{
		response.sendRedirect("http://362301.xc.365huaer.com/mobile/meeting/index.jsp?aid=7BX9sw1%2FQnY%3D&wuid=362301");
	}
	
}

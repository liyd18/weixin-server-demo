package net.jeeshop.web.action.manage.wht.wxcode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.chanjar.weixin.mp.api.WxMpService;
import net.jeeshop.core.ManageContainer;
import net.jeeshop.core.system.bean.User;
import net.jeeshop.services.manage.wht.product.ProductService;
import net.jeeshop.services.manage.wht.product.bean.Product;
import net.jeeshop.services.manage.wht.wxqrcode.QRCodeService;
import net.jeeshop.services.manage.wht.wxqrcode.bean.QRCodeBean;
import net.jeeshop.services.manage.wht.wxtoken.WxtokenService;
import net.jeeshop.services.manage.wht.wxtoken.bean.Wxtoken;
import net.jeeshop.web.util.QiniuUtils;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.qiniu.storage.model.FileInfo;

@Controller
@RequestMapping("wht/manage/qrcode")
public class QRCode {
	private final static Logger log = LoggerFactory.getLogger(QRCode.class);
	
	@Value("#{config[wxtoken_appID]}")
	private String appID;
	@Value("#{config[wxtoken_appScret]}")
	private String appScret;
	
	@Autowired
	private WxMpService wxMpService;
	@Resource(name = "whtWxtokenServiceManage")
	private WxtokenService service;
	@Resource
	private QRCodeService qrCodeService;
	@Resource(name = "whtProductServiceManage")
	private ProductService productService;
	/**
	 * 
	 * <p>Description: </br>
	 * @author zhangpf
	 * @param id
	 * @param username
	 * @param type 1:商品 ，2：生产厂家，为空则是会员
	 * @param code
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "getQRCode" , method = RequestMethod.POST)
	@ResponseBody
	public String getQRCode(String id, String username, String type, String code,
			HttpServletRequest request) throws Exception {
		QiniuUtils qiniu = new QiniuUtils();
		FileInfo f = qiniu.findOneFile(qiniu.getBucketName(), username);
		if(f!=null){
			log.info(">>>>>>>>>>>>>>>image is exist in qiniui");
			return username;
		}
		Wxtoken wx = service.selectOne(null);
		String token = null;
		if(wx==null){
			log.info("access_token为空,调getWXToken()重新获取token");
			token = getWXToken();
		}else{
			if(wx.getExpirestime().getTime()<new Date().getTime()){
				log.info("微信access_token过期,调getWXToken()重新获取token");
				token = getWXToken();
			}else{
				token = wx.getAccesstoken();
			}
		}
		log.info("access_token:"+token);
		String url = getTicket(token, id);
		if (StringUtils.isBlank(url)) {
			log.info(">>>>>>>>>>>>>>>>>>>40053:action_info参数不正确----重新生成token获取URL");
			token = getWXToken();
			url = getTicket(token, id);
		}
		if(StringUtils.isNotBlank(type) && "1".equals(type)){
			//判断商品是否上架
			Product p = productService.selectById(id);
			if(p == null || StringUtils.isBlank(p.getId())){
				return "noSale";
			}
			code = "p@" + code;
		}else if(StringUtils.isNotBlank(type) && "2".equals(type)){
			code = "f@" + code;
		}
		if(StringUtils.isNotBlank(type)){
			//获取后台用户信息
			User u = (User) request.getSession().getAttribute(ManageContainer.manage_session_user_info);
			Integer createUser = Integer.parseInt(u.getId());
			QRCodeBean q = new QRCodeBean();
			q.setStatus("0");
			q.setAppQRCode(code);
			q.setCreateUser(createUser);
			q.setWxQRCode(url);
			qrCodeService.insert(q);
		}
		String s = creatUrl(url, username, request);
		return s;
	}
	
	/**
	 * 
	 * <p>Description:调用浏览器下载二维码</br>
	 * @author liyd
	 * @param urlString 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping(value="downImage",method=RequestMethod.POST)
	public void downImage(String urlString,HttpServletRequest request,HttpServletResponse response) throws IOException{
        BufferedInputStream dis = null;
        BufferedOutputStream fos = null;
        String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);
        log.info("URL:"+urlString+"fileName:"+fileName);
        try {
            URL url = new URL(urlString);
            response.setContentType("application/x-msdownload;");  
            response.setHeader("Content-disposition", "attachment; filename=" + new String(fileName.getBytes("utf-8"), "ISO8859-1"));  
            response.setHeader("Content-Length", String.valueOf(url.openConnection().getContentLength()));  
             
            dis = new BufferedInputStream(url.openStream());
            fos = new BufferedOutputStream(response.getOutputStream());  
            byte[] buff = new byte[2048];  
            int bytesRead;  
            while (-1 != (bytesRead = dis.read(buff, 0, buff.length))) {  
                fos.write(buff, 0, bytesRead);  
            }  
            log.info(">>>>>>>>>>>>>>>>>>>download image success");
        } catch (Exception e) {
            e.printStackTrace();
        } finally { 
            if (dis != null)  
                dis.close();  
            if (fos != null)  
                fos.close();  
        }
	}

	/**
	 * 
	 * <p>
	 * Description:生成ticket</br>
	 * @author liyd
	 * @param access_token  令牌
	 * @param scene_str 场景值ID
	 * @return
	 */
	public static String getTicket(String access_token, String scene_str) {
		// 获取数据的地址（微信提供）
		String wxurl = "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token="
				+ access_token;
		// 发送给微信服务器的数据
		String jsonStr = "{\"action_name\": \"QR_LIMIT_STR_SCENE\", \"action_info\": {\"scene\": {\"scene_str\": "
				+ scene_str + "}}}";
		String response = sendPost(jsonStr, wxurl);
		JSONObject json = JSONObject.fromObject(response);
		String url = (String) json.get("url");
		if(StringUtils.isBlank(url)){
			log.info(">>>>>>>>>>>>>>>>>>>url is null:getTicket error-----"+json.getString("errmsg"));
		}
		return url;
	}

	public static String sendPost(String param, String url) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 设置通用的请求属性
			HttpURLConnection conn = (HttpURLConnection) realUrl
					.openConnection();
			conn.setRequestMethod("POST");// 设置请求方式
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);
			conn.setDoInput(true);	
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(new OutputStreamWriter(
					conn.getOutputStream(), "utf-8"));
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(
					conn.getInputStream(), "UTF-8"));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}
	/**
	 * 
	 * <p>Description:根据URL生成微信二维码</br>
	 * @author liyd
	 * @param param 微信URL
	 * @param username 文件名称
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public static String creatUrl(String param, String username,
			HttpServletRequest request) throws Exception {
		// 生成二维码
		Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
		// 指定纠错等级
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
		// 指定编码格式
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.MARGIN, 1);
		try {
			BitMatrix bitMatrix = new MultiFormatWriter().encode(param,
					BarcodeFormat.QR_CODE, 250, 250, hints);

			String realPath = request
					.getSession()
					.getServletContext()
					.getRealPath(
							File.separator + "images" + File.separator
									+ "wxqcimg");
			File file =  new File(realPath);
			if(!file.exists()){
				file.mkdirs();
			}
			String FilePath = realPath + File.separator + username + ".jpg";// 生成的二维码要存放的文件路径

			isexitsPath(FilePath);
			File f = new File(FilePath);
			if(f.exists()){
				log.info(">>>>>>>>>>>>>>>>>>>image is exist in server");
			}else{
				OutputStream out = new FileOutputStream(f);
				MatrixToImageWriter.writeToStream(bitMatrix, "jpg", out);// 输出二维码
				out.flush();
				out.close();
			}

			// 七牛保存
			QiniuUtils qiniu = new QiniuUtils();
			String response = qiniu.uploadFile(realPath + File.separator, username + ".jpg",
						qiniu.getBucketName());
			log.info(">>>>>>>>>>>>>>>>>>>image is upload success------response:"+response);
			
			//上传成功后删除本地文件	
			File fi = new File(FilePath);
			if(fi.exists() && fi.isFile()){
				fi.delete();
				log.info("删除成功");
			}
			
		} catch (WriterException e) {
			e.printStackTrace();
		}
		return username;
	}

	public static void isexitsPath(String FilePath) {
		System.out.println(FilePath);
		String[] paths = FilePath.split("\\\\");
		StringBuffer fullPath = new StringBuffer();
		for (int i = 0; i < paths.length; i++) {
			fullPath.append(paths[i]).append("\\\\");
			File file = new File(fullPath.toString());
			if (paths.length - 1 != i) {// 判断path到文件名时，无须继续创建文件夹！
				if (!file.exists()) {
					file.mkdir();
					System.out.println("创建目录为：" + fullPath.toString());
				}
			}
		}
	}
	
	/**
	 * <p>Description:当定时器失效时获取微信access_token</br>
	 * @author liyd
	 * @return
	 */
	public String getWXToken(){
		String token = null;
		try {
			token = wxMpService.getAccessToken(true);
			Wxtoken w = new Wxtoken();
			w.setAppid(appID);
			w.setAccesstoken(token);
			w.setExpires(3600);
			long timeNow= System.currentTimeMillis();
			long timeLose=System.currentTimeMillis()+60*60*1000;
			w.setCreatetime(new Date(timeNow));
			w.setExpirestime(new Date(timeLose));
			service.insert(w);
			log.info(">>>>>>>>>>>>>>>>>>>access_token:"+token);
			return token;
		} catch (Exception e) {
				e.printStackTrace();
		}
		return token;
	}
}

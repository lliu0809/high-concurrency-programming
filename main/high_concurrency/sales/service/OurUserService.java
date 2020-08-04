package high_concurrency.sales.service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import high_concurrency.sales.dao.OurUserDao;
import high_concurrency.sales.domain.OurUser;
import high_concurrency.sales.exception.GlobalException;
import high_concurrency.sales.redis.OurUserKey;
import high_concurrency.sales.redis.RedisService;
import high_concurrency.sales.result.CodeMsg;
import high_concurrency.sales.util.MD5Util;
import high_concurrency.sales.util.UUIDUtil;
import high_concurrency.sales.vo.LoginVo;

@Service
public class OurUserService {
	
	
	public static final String COOKI_NAME_TOKEN = "token";
	
	@Autowired
	OurUserDao miaoshaUserDao;
	
	@Autowired
	RedisService redisService;
	
	public OurUser getById(long id) {
		return miaoshaUserDao.getById(id);
	}
	

	public OurUser getByToken(HttpServletResponse response, String token) {
		if(StringUtils.isEmpty(token)) {
			return null;
		}
		OurUser user = redisService.get(OurUserKey.token, token, OurUser.class);
		if(user != null) {
			addCookie(response, token, user);
		}
		return user;
	}
	

	public boolean login(HttpServletResponse response, LoginVo loginVo) {
		if(loginVo == null) {
			throw new GlobalException(CodeMsg.SERVER_ERROR);
		}
		String mobile = loginVo.getMobile();
		String formPass = loginVo.getPassword();
		// is phone number in our database
		OurUser user = getById(Long.parseLong(mobile));
		if(user == null) {
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}
		//验证密码
		String dbPass = user.getPassword();
		String saltDB = user.getSalt();
		String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
		if(!calcPass.equals(dbPass)) {
			throw new GlobalException(CodeMsg.PASSWORD_ERROR);
		}
		//生成cookie
		String token = UUIDUtil.uuid();
		addCookie(response, token, user);
		return true;
	}
	
	private void addCookie(HttpServletResponse response, String token, OurUser user) {
		redisService.set(OurUserKey.token, token, user);
		Cookie cookie = new Cookie(COOKI_NAME_TOKEN, token);
		cookie.setMaxAge(OurUserKey.token.expireSeconds());
		cookie.setPath("/");
		response.addCookie(cookie);
	}

}
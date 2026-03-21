package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl.createTime;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties wechatProperties;
    @Autowired
    private UserMapper userMapper;

    // 微信接口服务
    private final String Wx_Login_Url = "https://api.weixin.qq.com/sns/jscode2session";

    //
    /**
     * 微信用户登录
     * @param userLoginDTO
     * @return
     */
    public User login(UserLoginDTO userLoginDTO) {
        //调用微信接口服务获取openid
        String openid = getOpenId(userLoginDTO.getCode());
        //判断openid是否为空，为空抛出异常
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //根据openid查询用户信息
        User user = userMapper.getByOpenId(openid);
        // 如果用户不存在，则创建用户
        if (user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回用户对象
        return user;
    }
    /**
     * 调用微信接口服务，获取openid
     * @param code
     * @return
     */
    private String getOpenId(String code){
        Map<String, String> map = new HashMap<>();
        map.put("appid",wechatProperties.getAppid());
        map.put("secret",wechatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(Wx_Login_Url, map);
        JSONObject jsonObject = JSONObject.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}

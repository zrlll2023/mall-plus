package com.zrl.rlmall.service.impl;

import com.zrl.rlmall.entity.UserInfo;
import com.zrl.rlmall.mapper.UserInfoMapper;
import com.zrl.rlmall.service.UserInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

}

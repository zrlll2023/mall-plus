package com.zrl.rlmall.service.impl;

import com.zrl.rlmall.entity.BlogComments;
import com.zrl.rlmall.mapper.BlogCommentsMapper;
import com.zrl.rlmall.service.BlogCommentsService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements BlogCommentsService {

}

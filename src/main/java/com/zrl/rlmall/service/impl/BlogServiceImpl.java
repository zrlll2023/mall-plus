package com.zrl.rlmall.service.impl;

import com.zrl.rlmall.entity.Blog;
import com.zrl.rlmall.mapper.BlogMapper;
import com.zrl.rlmall.service.BlogService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {

}

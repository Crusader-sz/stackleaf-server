package com.crusader.stackleafserver.service.support;

import cn.dev33.satoken.stp.StpUtil;
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.enumeration.UserRole;
import com.crusader.stackleafserver.exception.BusinessException;
import com.crusader.stackleafserver.mapper.UserMapper;
import com.crusader.stackleafserver.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Arrays;

@Component
public class UserAccess {
    @Autowired
    private UserMapper userMapper;

    public User currentUser() {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().eq(User::getId, StpUtil.getLoginIdAsLong());
        // 写事务锁住账号，禁止删除/禁用与该账号创建关联记录交错。
        if (TransactionSynchronizationManager.isActualTransactionActive()) query.last("FOR UPDATE");
        User user = userMapper.selectOne(query);
        if (user == null) {
            throw new BusinessException(ResultCodeConstant.UNAUTHORIZED, MessageConstant.USER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCodeConstant.FORBIDDEN, MessageConstant.ACCOUNT_DISABLED);
        }
        return user;
    }

    /** 多用户写操作按主键统一加锁，避免相互关注时锁顺序相反。 */
    public void lockUsers(Long... ids) {
        List<Long> ordered = Arrays.stream(ids).distinct().sorted().toList();
        userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, ordered)
                .orderByAsc(User::getId).last("FOR UPDATE"));
    }

    public boolean isAdmin(User user) {
        return Integer.valueOf(UserRole.ADMIN.getCode()).equals(user.getRole());
    }

    public void requireAdmin() {
        if (!isAdmin(currentUser())) {
            throw new BusinessException(ResultCodeConstant.FORBIDDEN, MessageConstant.ADMIN_REQUIRED);
        }
    }
}

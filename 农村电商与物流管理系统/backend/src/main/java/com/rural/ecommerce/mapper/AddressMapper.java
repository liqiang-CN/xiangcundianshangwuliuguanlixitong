package com.rural.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {

    /**
     * 将用户的所有地址设为非默认
     */
    @Update("UPDATE user_address SET is_default = 0 WHERE user_id = #{userId} AND deleted = 0")
    int clearDefaultByUserId(@Param("userId") Long userId);
}

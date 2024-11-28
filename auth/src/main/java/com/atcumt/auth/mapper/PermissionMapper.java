package com.atcumt.auth.mapper;

import com.atcumt.model.auth.dto.SortedPermissionDTO;
import com.atcumt.model.auth.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    List<Permission> selectPermissionsByUserId(String userId);

    List<Permission> selectPermissionsByRoleId(String roleId);

    @Select("""
            SELECT
                module,
                permissions
            FROM (
                SELECT
                    SUBSTRING_INDEX(permission_name, '.', 1) AS module,
                    GROUP_CONCAT(
                        CONCAT(permission_id, '||', permission_name, '||', description, '||', create_time, '||', update_time)
                        ORDER BY SUBSTRING_INDEX(permission_name, '.', -1) ASC
                        SEPARATOR '#|#'
                    ) AS permissions
                FROM
                    permission
                GROUP BY
                    module
                ORDER BY
                    module
            ) AS aggregated_permissions
            """)
    Page<SortedPermissionDTO> selectSortedPermissionsPage(IPage<SortedPermissionDTO> page);
}

package com.example.oauth.server.web.role;

import com.example.oauth.server.common.restful.RestfulVo;
import com.example.oauth.server.common.restful.ResultUtil;
import com.example.oauth.server.domain.role.dto.RoleDTO;
import com.example.oauth.server.service.role.RoleService;
import com.example.oauth.server.web.base.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/***
 * 角色 controller
 */
@RestController
public class RoleController extends AbstractController {

    @Autowired
    private RoleService roleService;

    /**
     * 新增数据
     * @param roleDTO
     * @return
     */
    @PostMapping("role")
    public RestfulVo saveRole(RoleDTO roleDTO){
        boolean success = this.roleService.save(roleDTO);
        return ResultUtil.restful(success);
    }
}

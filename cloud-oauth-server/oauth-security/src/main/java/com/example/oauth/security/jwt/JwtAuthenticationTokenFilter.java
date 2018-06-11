package com.example.oauth.security.jwt;

import com.example.oauth.security.handlerinterceptor.MyAuthenticationException;
import com.example.oauth.server.common.exception.DescribeException;
import com.example.oauth.server.common.exception.ErrorCodeEnum;
import com.example.oauth.server.common.redis.RedisKeys;
import com.example.oauth.server.common.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/***
 *
 * @FileName: JwtAuthenticationTokenFilter
 * @Company:
 * @author    ljy
 * @Date      2018年05月120日
 * @version   1.0.0
 * @remark:   jwt认证token
 * @explain   每次请求接口时 就会进入这里验证token 是否合法
 *             token 如果用户一直在操作，则token 过期时间会叠加    如果超过设置的过期时间未操作  则token 失效 需要重新登录
 *
 */
@Slf4j
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${jwt.header}")
    private String tokenHeader;

    @Value("${jwt.tokenHead}")
    private String tokenHead;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = httpServletRequest.getHeader(this.tokenHeader);
        if (authHeader != null && authHeader.startsWith(tokenHead)) {
            final String authToken = authHeader.substring(tokenHead.length()); // The part after "Bearer "
            // 查看redis中的token信息是否过期
            boolean isExists = redisUtil.hexists(RedisKeys.USER_KEY,authToken);
            if (!isExists){
                // token 过期 提示用户登录超时 重新登录系统
                //throw  new MyAuthenticationException(ErrorCodeEnum.LOGIN_WITHOUT);
            }

            //如果在token过期之前触发接口,我们更新token过期时间，token值不变只更新过期时间
            Date createTokenDate = jwtTokenUtil.getCreatedDateFromToken(authToken);  //获取token生成时间
            log.info("createTokenDate: " + createTokenDate);
            if(createTokenDate != null){
                Duration between = Duration.between(Instant.now(), Instant.ofEpochMilli(createTokenDate.getTime()));
                Long differSeconds = between.toMillis();
                boolean isExpire = expiration > differSeconds;
                if (isExpire) {  //如果 请求接口时间在token 过期之前 则更新token过期时间  我们可以将用户的token 存放到redis 中,更新redis 的过期时间

                    //更新 延长 redis 中的过期时间


                }
            }



            String useraccount = jwtTokenUtil.getUsernameFromToken(authToken);
            log.info("JwtAuthenticationTokenFilter[doFilterInternal] checking authentication " + useraccount);

            //token校验通过
            if (useraccount != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                //根据account去数据库中查询user数据，足够信任token的情况下，可以省略这一步
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(useraccount);

                if (jwtTokenUtil.validateToken(authToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(
                            httpServletRequest));
                    log.info("JwtAuthenticationTokenFilter[doFilterInternal]  authenticated user " + useraccount + ", setting security context");
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
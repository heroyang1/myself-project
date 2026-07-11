package com.backstage.web.controller.pc;

import com.backstage.common.constant.HttpStatus;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * PC端异常处理器
 * 处理C端(前台)接口的异常,返回统一的R格式响应
 *
 * @author backstage
 */
@RestControllerAdvice(basePackages = {
        "com.backstage.web.controller.pc",
        "com.backstage.system.controller.group",
        "com.backstage.system.controller.redeem",
        "com.backstage.system.controller.servergroup",
        "com.backstage.system.controller.order"
})
@Order(1)
public class PcExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(PcExceptionHandler.class);

    /**
     * 业务异常(如:拼团活动不存在、用户已参团等)
     * 使用WARN级别日志(601),不打印堆栈信息
     */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}',发生业务异常:{}", requestURI, e.getMessage());
        // 业务异常返回601状态码,而非500
        return R.fail(HttpStatus.WARN, e.getMessage());
    }

    /**
     * 系统异常(如:空指针、数据库连接失败等)
     * 使用ERROR级别日志,打印完整堆栈信息
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常", requestURI, e);
        return R.fail("系统错误，请联系管理员");
    }
}

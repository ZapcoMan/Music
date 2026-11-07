package com.shanzhu.music.exception;

import com.shanzhu.music.common.R;
import com.shanzhu.music.common.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * 本轮的改动只有一件：13 个 handler 统一返回 R&lt;Void&gt;。此前它们全是 raw R——
 * Controller 侧的 raw R 已经清零，这里却漏了。raw 会让调用点失去泛型检查，
 * 而异常响应恰恰是前端最需要确定形状的那一类返回体：它决定
 * res.success / res.code / res.data 三个键在失败时是否仍然可预期。
 *
 * handleBusinessException 的用法问题（零 throw）单独记在它自己的注释里，不在这里重复。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * 需要说清一件事：BusinessException 目前在全仓（含测试）**零 throw 点**。业务失败走的是
     * return R.error(...)，服务层的参数与状态问题走 IllegalArgumentException /
     * IllegalStateException，都不经过这里；JaCoCo 也显示该类 0% 覆盖。
     *
     * 所以这条通道现在只是"看起来有一套业务异常约定"。保留还是删除是设计决策，本轮不替你定：
     * 若保留，建议把服务层现有的 IllegalArgumentException 迁过来，让"失败怎么表达"收敛成一种；
     * 若确认不需要，连这个 handler 一起删掉更干净，别留一条装饰性的通道。
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常(@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        // code 统一取 ResultCodeEnum.PARAM_ERROR，不再与 HTTP 状态码 400 混用；
        // 协议语义仍由 @ResponseStatus 决定
        return R.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定异常: {}", message);
        return R.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束违反异常: {}", message);
        return R.error(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public R<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超限: {}", e.getMessage());
        return R.error(413, "文件大小超过限制");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage());
        return R.error(ResultCodeEnum.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 请求方法不匹配，例如用 GET 调已被改为 @DeleteMapping 的接口。
     *
     * 此前没有专门处理器，它落到最后的 handleException 变成 500：
     * 客户端看到的是"系统异常"，而真实原因是方法用错，排查方向被彻底带偏 ——
     * 本轮修测试用例时正是被这个 500 误导，先去看数据而不是动词。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                      jakarta.servlet.http.HttpServletRequest request) {
        log.warn("请求方法不支持 - method={}, uri={}, 支持={}",
                e.getMethod(), request.getRequestURI(),
                String.valueOf(e.getSupportedHttpMethods()));
        return R.error(405, "请求方法不支持: " + e.getMethod());
    }

    /**
     * 请求体 Content-Type 不被支持（需要 JSON 却发了表单等）。
     * 同属协议层问题，不该记成系统异常。
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public R<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("Content-Type 不支持: {}", e.getContentType());
        return R.error(415, "不支持的请求类型: " + e.getContentType());
    }

    /**
     * 必填请求参数缺失。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName());
        return R.error(ResultCodeEnum.PARAM_ERROR.getCode(), "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 静态资源未找到（favicon、误访问目录等）。
     * Spring Boot 3.4 会抛 NoResourceFoundException，落到通用分支就成了 500，
     * 于是一个 favicon 请求就能在日志里制造一条"系统异常"。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("静态资源未找到: {}", e.getResourcePath());
        return R.error(404, "资源不存在");
    }

    /**
     * 没有匹配的处理器（仅在未启用静态资源兜底时抛出）。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("接口不存在 - {} {}", e.getHttpMethod(), e.getRequestURL());
        return R.error(404, "接口不存在");
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return R.error(ResultCodeEnum.NULL_POINT.getCode(), "系统内部错误");
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.error(ResultCodeEnum.UNKNOWN_ERROR.getCode(), "系统异常，请联系管理员");
    }

}

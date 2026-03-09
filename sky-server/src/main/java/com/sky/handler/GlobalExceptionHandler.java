package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error("异常信息：{}", ex.getMessage());
        // 处理重复用户名异常
        //Duplicate entry 'admin' for key 'employee.username'
        //TODO 后续将此异常处理转到service层按照自定义业务异常处理，由service层抛出异常
        if(ex.getMessage().contains("Duplicate entry")){
            String username = ex.getMessage().split(" ")[2];
            return Result.error(username + "已存在");
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }
}

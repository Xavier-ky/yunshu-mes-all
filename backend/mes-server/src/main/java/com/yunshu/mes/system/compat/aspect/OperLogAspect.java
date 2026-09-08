package com.yunshu.mes.system.compat.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.OperlogCompatRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class OperLogAspect {

    private final OperlogCompatRepository operlogRepo;
    private final ObjectMapper objectMapper;

    public OperLogAspect(OperlogCompatRepository operlogRepo, ObjectMapper objectMapper) {
        this.operlogRepo = operlogRepo;
        this.objectMapper = objectMapper;
    }

    @Pointcut("@annotation(com.yunshu.mes.system.compat.annotation.OperLog)")
    public void operLogPointcut() {}

    @AfterReturning(pointcut = "operLogPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        saveLog(joinPoint, result, null);
    }

    @AfterThrowing(pointcut = "operLogPointcut()", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
        saveLog(joinPoint, null, ex);
    }

    private void saveLog(JoinPoint joinPoint, Object result, Throwable ex) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            OperLog operLog = sig.getMethod().getAnnotation(OperLog.class);
            if (operLog == null) {
                return;
            }
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            String operName = SysCompatHelper.currentUsername();
            String param = "";
            if (joinPoint.getArgs().length > 0) {
                try {
                    param = objectMapper.writeValueAsString(joinPoint.getArgs()[0]);
                    if (param.length() > 1900) {
                        param = param.substring(0, 1900);
                    }
                } catch (Exception ignored) {
                    param = "";
                }
            }
            String jsonResult = "";
            if (result != null) {
                try {
                    jsonResult = objectMapper.writeValueAsString(result);
                    if (jsonResult.length() > 1900) {
                        jsonResult = jsonResult.substring(0, 1900);
                    }
                } catch (Exception ignored) {
                    jsonResult = "";
                }
            }
            int status = ex == null ? 0 : 1;
            String errorMsg = ex == null ? "" : (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            if (errorMsg.length() > 1900) {
                errorMsg = errorMsg.substring(0, 1900);
            }
            operlogRepo.insert(buildLogMap(operLog, joinPoint, request, operName, param, jsonResult, status, errorMsg));
        } catch (Exception ignored) {
            // 日志写入失败不影响业务
        }
    }

    private Map<String, Object> buildLogMap(OperLog operLog, JoinPoint joinPoint, HttpServletRequest request,
                                            String operName, String param, String jsonResult, int status, String errorMsg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", operLog.title());
        m.put("businessType", operLog.businessType());
        m.put("method", joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName() + "()");
        m.put("requestMethod", request.getMethod());
        m.put("operName", operName);
        m.put("operUrl", request.getRequestURI());
        m.put("operIp", request.getRemoteAddr() == null ? "" : request.getRemoteAddr());
        m.put("operParam", param);
        m.put("jsonResult", jsonResult);
        m.put("status", status);
        m.put("errorMsg", errorMsg);
        return m;
    }
}

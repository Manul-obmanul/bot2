package runner.controller;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Aspect
@Slf4j
public class ServiceLogAspect {
    @Pointcut("execution(public * runner.service..*(..))")
    public void callService() { }

    @Before("callService()")
    public void beforeCallService(JoinPoint joinPoint) {
        List<String> args = Arrays.stream(joinPoint.getArgs())
                .map(Object::toString)
                .toList();
        String  str = joinPoint.getThis().toString();
        log.info("Call {} with args {} from {}", joinPoint.getSignature().getName(), args, str.substring(0, str.indexOf("@")));
    }

    @AfterReturning(value = "callService()", returning ="object")
    public void afterCallService(JoinPoint joinPoint, Object object) {
        log.info("Call {} with return {}", joinPoint.getSignature().getName(), object);
    }
}

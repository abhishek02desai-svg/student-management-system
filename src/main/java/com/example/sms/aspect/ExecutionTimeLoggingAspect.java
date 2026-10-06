package com.example.sms.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP (Aspect Oriented Programming): measures how long every service method takes
 * WITHOUT touching the service code.
 *
 * @Around runs before AND after the real method; proceed() is the real method call.
 * Pointcut = every method of every class in the service.impl package.
 */
@Slf4j
@Aspect
@Component
public class ExecutionTimeLoggingAspect {

    private static final long SLOW_LIMIT_MS = 1000;

    @Around("execution(* com.example.sms.service.impl..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.nanoTime();
        try {
            return joinPoint.proceed();          // run the actual service method
        } finally {                              // finally => time is logged even if it throws
            long timeMs = (System.nanoTime() - start) / 1_000_000;
            String method = joinPoint.getSignature().getDeclaringType().getSimpleName()
                    + "." + joinPoint.getSignature().getName() + "()";

            if (timeMs > SLOW_LIMIT_MS) {
                log.warn("SLOW: {} took {} ms", method, timeMs);
            } else {
                log.info("{} executed in {} ms", method, timeMs);
            }
        }
    }
}

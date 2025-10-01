package com.kukkalli.aaa.audit;

import com.kukkalli.aaa.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result = pjp.proceed(); // only audit on success

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Object[] args = pjp.getArgs();

        EvaluationContext ctx = buildContext(method, args, result);

        // Allow SpEL in action/targetType/targetId if they start with '#', else treat as literals
        String action     = resolve(audited.action(), ctx);
        String targetType = resolve(audited.targetType(), ctx);
        String targetId   = resolve(audited.targetId(), ctx);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("method", sig.toShortString());
        if (!isBlank(targetType)) details.put("targetType", targetType);
        if (!isBlank(targetId))   details.put("targetId", targetId);

        auditService.audit(action, details);
        return result;
    }

    // ---------------------------- helpers ----------------------------

    private static EvaluationContext buildContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        // by index: #p0, #p1 ...
        for (int i = 0; i < args.length; i++) {
            ctx.setVariable("p" + i, args[i]);
        }
        // by name: #argName (if debug info / -parameters present or Lombok recorded)
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }
        // whole args array
        ctx.setVariable("args", args);
        // return value
        ctx.setVariable("result", result);
        return ctx;
    }

    private static String resolve(String value, EvaluationContext ctx) {
        if (isBlank(value)) return value;
        if (value.startsWith("#")) {
            Object evaluated = PARSER.parseExpression(value).getValue(ctx);
            return evaluated == null ? null : String.valueOf(evaluated);
        }
        return value; // literal
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
